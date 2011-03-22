/*
 * Copyright 2009 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semispace.semimeter.dao;

import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.ArrayQuery;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.ParameterizedQuery;
import org.semispace.semimeter.space.CounterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service("semimeterDao")
public class SemiMeterDao implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDao.class);

    private SimpleJdbcTemplate jdbcTemplate;
    private SemiEventRegistration chRegistration;
    private SemiEventRegistration pqRegistration;
    private SemiEventRegistration aqRegistration;

    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    private SemiSpaceInterface space;
    private static final int MAX_PATH_LENGTH = 2048;

    @Autowired
    @Qualifier("semiMeterDataSource")
    public void setDataSource(DataSource semiMeterDataSource) {
        this.jdbcTemplate = new SimpleJdbcTemplate(semiMeterDataSource);
    }

    /**
     * @return size of stalegroup table, or -1 if any errors occur.
     */
    public int size() {
        int result = -1;
        rwl.readLock().lock();
        try {
            result = this.jdbcTemplate.queryForInt("select count(*) from meter");
        } catch (DataAccessException e) {
            log.warn("Table probably not yet created. Got (intentionally masked) " + e.getMessage());
        } finally {
            rwl.readLock().unlock();
        }

        return result;
    }

    public boolean isAlive() {
        rwl.readLock().lock();
        try {
            int result = this.jdbcTemplate.queryForInt("select count(*) from meter where updated < 1");
            if (result >= 0) {
                return true;
            }
        } catch (DataAccessException e) {
            log.warn("Table probably not yet created. Got (intentionally masked) " + e.getMessage());
        } finally {
            rwl.readLock().unlock();
        }
        return false;
    }

    /**
     * Method called from Spring. Will (try to) create the table with the meter, if it does not already exist.
     */
    public void afterPropertiesSet() {
        log.debug("Retrieving semispace.");
        space = SemiSpace.retrieveSpace();
        log.debug("Registering listeners.");
        if (chRegistration != null || pqRegistration != null) {
            log.error("Did not expect any SemiSpace registration to exist already. Not registering again");
        } else {
            SpacePQListener spacePqListener = new SpacePQListener(space, this, "Query listener - both parameterized and array queries");
            // Listen for events ten years
            chRegistration = space.notify(new CounterHolder(), new Space2Dao(space, this, "CounterHolder which holds elements to be counted"), SemiSpace.ONE_DAY * 3650);
            // Reusing
            pqRegistration = space.notify(new ParameterizedQuery(), spacePqListener, SemiSpace.ONE_DAY * 3650);
            aqRegistration = space.notify(new ArrayQuery(), spacePqListener, SemiSpace.ONE_DAY * 3650);

        }

        if (size() < 0) {
            log.info("Creating table meter");
            // The data type integer in the database is a long in the java world.
            try {
                jdbcTemplate.getJdbcOperations().execute("create table meter(updated bigint NOT NULL, counted integer NOT NULL, path varchar(" + MAX_PATH_LENGTH + ") NOT NULL)");
            } catch (Exception e) {
                try {
                    // Probably a different database
                    jdbcTemplate.getJdbcOperations().execute("create table meter(updated integer NOT NULL, counted integer NOT NULL, path varchar(" + MAX_PATH_LENGTH + ") NOT NULL)");
                } catch (Exception e2) {
                    log.error("Did not manage to create table meter?! First exception is masked: " + e.getMessage(), e2);
                }
            }
        }
        if (size() < 1) {
            try {
                // Need an initial default value
                jdbcTemplate.getJdbcOperations().execute("insert into meter(updated, counted, path) values (1, 0, '__disregarded needed default__')");
            } catch (Exception e) {
                log.error("Could not create default?!", e);
            }
        }
        try {
            log.debug("Creating indexes (even if they already exist)");
            // Create indexes
            jdbcTemplate.getJdbcOperations().execute("create index meter_updt_ix on meter( updated )");
            jdbcTemplate.getJdbcOperations().execute("create index meter_path_ix on meter( path )");
        } catch (Exception e) {
            log.error("Did not manage to create index on updated field. This is probably as it already exists. " +
                    "Ignoring this, as we ALWAYS try to create indexes after restart. Masked exception: " + e);
        }
        if (size() > 1) {
            // We don't need default any more if we have data
            jdbcTemplate.getJdbcOperations().execute("DELETE FROM meter where updated=1 and path like '__disregarded needed default__'");
        }
        if (isAlive()) {
            try {
                jdbcTemplate.getJdbcOperations().execute("select count(count) from meter meter");
                log.warn("Renaming field count to counted in table meter.");
                try {
                    jdbcTemplate.getJdbcOperations().execute("ALTER TABLE meter CHANGE COLUMN count counted integer NOT NULL;");
                } catch (Exception e) {
                    log.error("Could not rename column count to counted. Please drop table or rename column manually");
                }
            } catch (Exception e) {
                // Expected
            }
        }
    }

    protected void performInsertion(Collection<Item> items) {
        //log.debug("Performing batch insertion of "+items.size()+" items.");
        SqlParameterSource[] insertArgs = SqlParameterSourceUtils.createBatch(items.toArray());
        List<Object[]> updateArgs = new ArrayList<Object[]>();

        for (Item item : items) {
            // Original just called insert
            updateArgs.add(new Object[]{item.getAccessNumber(), item.getPath(), item.getWhen()});
        }
        rwl.writeLock().lock();
        try {
            try {
                //log.debug("INSERT INTO meter(updated, count, path) SELECT DISTINCT ?, 0, ? FROM meter WHERE NOT EXISTS ( SELECT * FROM meter WHERE updated=? AND path=?)");
                jdbcTemplate.batchUpdate("INSERT INTO meter(updated, counted, path) SELECT DISTINCT :when, 0, :path FROM meter WHERE NOT EXISTS ( SELECT * FROM meter WHERE updated=:when AND path=:path)",
                        insertArgs);
            } catch (Exception e) {
                log.warn("Unlikely event occurred - failure whilst inserting priming elements. This is not overly critical. Masked exception: " + e);
            }
            jdbcTemplate.batchUpdate("update meter SET counted=counted+? WHERE path like ? and updated=?",
                    updateArgs);
        } catch (Exception e) {
            log.error("Could not update elements", e);
        } finally {
            rwl.writeLock().unlock();
        }
    }


    private void failed_rewrite_performInsertion(Collection<Item> items) {
        //log.debug("Performing batch insertion of "+items.size()+" items.");
        //List<Object[]> insertArgs = new ArrayList<Object[]>();
        //List<Object[]> updateArgs = new ArrayList<Object[]>();

        SqlParameterSource[] insertArgs = SqlParameterSourceUtils.createBatch(items.toArray());
        SqlParameterSource[] updateArgs = SqlParameterSourceUtils.createBatch(items.toArray());

        //for ( Item item : items ) {
        // Original just called insert
        //insertArgs.add( new Object[]{item.getWhen(), item.getPath(),item.getWhen(), item.getPath()});
        //updateArgs.add( new Object[] {item.getAccessNumber(), item.getPath(), item.getWhen()});
        //}
        rwl.writeLock().lock();
        try {
            try {
                //log.debug("INSERT INTO meter(updated, count, path) SELECT DISTINCT ?, 0, ? FROM meter WHERE NOT EXISTS ( SELECT * FROM meter WHERE updated=? AND path=?)");
                jdbcTemplate.batchUpdate("INSERT INTO meter(updated, counted, path) SELECT DISTINCT :when, 0, :path FROM meter WHERE NOT EXISTS ( SELECT * FROM meter WHERE updated=:when AND path=:path)",
                        insertArgs);
            } catch (Exception e) {
                log.warn("Unlikely event occurred - failure whilst inserting priming elements. This is not overly critical. Masked exception: " + e);
            }
            jdbcTemplate.batchUpdate("update meter SET counted=counted+:accessNumber WHERE :path like :path and updated=:when",
                    updateArgs);
        } catch (Exception e) {
            log.error("Could not update elements", e);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Insertion is performed like this: Try to insert item with count of zero if does not already
     * exist. Then update the count to the correct value. The candidate key for the element is
     * (when + path). This method is now legacy.
     * <p/>
     * Used in junit test for comparison purpose. For this reason is the method protected.
     *
     * @deprecated Do not use this code other places than test
     */
    protected void insert(Item item) {
        rwl.writeLock().lock();
        try {
            try {
                jdbcTemplate.update("INSERT INTO meter(updated, counted, path) SELECT DISTINCT ?, 0, ? FROM meter WHERE NOT EXISTS ( SELECT * FROM meter WHERE updated=? AND path=?)",
                        new Object[]{item.getWhen(), item.getPath(), item.getWhen(), item.getPath()});
            } catch (Exception e) {
                log.warn("Unlikely event occurred - failure whilst inserting priming element", e);
            }
            jdbcTemplate.update(
                    "update meter SET counted=counted+? WHERE path like ? and updated=?",
                    new Object[]{item.getAccessNumber(), item.getPath(), item.getWhen()});
        } catch (Exception e) {
            log.error("Could not insert or update", e);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void destroy() throws Exception {
        if (chRegistration != null) {
            chRegistration.getLease().cancel();
            chRegistration = null;
        }
        if (pqRegistration != null) {
            pqRegistration.getLease().cancel();
            pqRegistration = null;
        }
        if (aqRegistration != null) {
            aqRegistration.getLease().cancel();
            aqRegistration = null;
        }
    }

    public Long sumItems(long startAt, long endAt, String path) {
        Long result = Long.valueOf(-1);
        rwl.readLock().lock();
        try {
            final String sql = "select sum(counted) from meter " +
                    "WHERE " +
                    "updated>? AND updated<=?  AND path like ?";
            //log.debug("Querying with ("+startAt+","+endAt+","+path+") : "+sql);
            Long sum = Long.valueOf(jdbcTemplate.queryForLong(sql,
                    new Object[]{Long.valueOf(startAt), Long.valueOf(endAt), path}));
            result = sum;
        } finally {
            rwl.readLock().unlock();
        }

        return result;
    }

    public JsonResults[] performParameterizedQuery(long startAt, long endAt, String path) {
        if (path.indexOf("$") == -1 || path.indexOf("$") != path.lastIndexOf("$")) {
            throw new RuntimeException("Expecting one and only one $");
        }
        List<JsonResults> jrs = new ArrayList<JsonResults>();
        rwl.readLock().lock();
        try {
            String prefix = path.substring(0, path.indexOf("$"));
            String postfix = path.substring(path.indexOf("$") + 1);
            List<String> variants = createStringListOfVariants(startAt, endAt, path);
            log.trace("Got variants: {}", variants);
            for (String s : variants.toArray(new String[0])) {
                Long sum = sumItems(startAt, endAt, prefix + s + postfix);
                JsonResults jr = new JsonResults();
                jr.setKey(s);
                jr.setValue(sum.toString());
                jrs.add(jr);
            }
        } finally {
            rwl.readLock().unlock();
        }

        return jrs.toArray(new JsonResults[0]);
    }

    private List<String> createStringListOfVariants(long startAt, long endAt, String path) {
        List<String> list = new ArrayList<String>();
        rwl.readLock().lock();
        try {
            String prefix = path.substring(0, path.indexOf("$"));
            String postfix = path.substring(path.indexOf("$") + 1);
            String sql = "SELECT distinct path AS path FROM meter WHERE path like ? " +
                    "AND path like ? AND updated>? AND updated<=? ORDER BY path";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql,
                    new Object[]{prefix + "%",
                            "%" + postfix + "%",
                            Long.valueOf(startAt), Long.valueOf(endAt)});

            //log.debug("Got "+result.size()+" results when doing "+sql+" with regards to ("+prefix+","+postfix+", "+startAt+","+endAt+")");
            for (Map<String, Object> m : result) {
                String s = (String) m.get("path");
                s = s.substring(prefix.length(), s.length() - postfix.length());
                list.add(s);
            }
        } finally {
            rwl.readLock().unlock();
        }

        return list;
    }

    /**
     * @param numberOfSamples Presuming number to be positive
     */
    public JsonResults[] createTimeArray(String path, long endAt, long startAt, Integer numberOfSamples) {
        rwl.readLock().lock();
        List<Map<String, Object>> list = null;
        try {
            final String sql = "SELECT updated, counted FROM meter " +
                    "WHERE " +
                    "updated>? AND updated<=?  AND path like ? ORDER BY updated";
            //log.debug("Querying with ("+startAt+","+endAt+","+path+") : "+sql);
            list = jdbcTemplate.queryForList(sql,
                    new Object[]{Long.valueOf(startAt), Long.valueOf(endAt), path});
        } finally {
            rwl.readLock().unlock();
        }
        // Need to add the start and stop in order to get the array correctly bounded.
        Map<String, Object> fake = new HashMap<String, Object>();
        fake.put("updated", "" + startAt);
        fake.put("counted", "0");
        list.add(0, fake);
        fake = new HashMap();
        fake.put("updated", "" + endAt);
        fake.put("counted", "0");
        list.add(fake);
        List<JsonResults> result = flatten(list, numberOfSamples.intValue());

        return result.toArray(new JsonResults[0]);
    }

    /**
     * Protected for the benefit of junit test
     */
    protected List<JsonResults> flatten(List<Map<String, Object>> list, int num) {
        // Using AtomicInteger just because it has nice adding features.
        List<AtomicInteger> res = new ArrayList<AtomicInteger>(num);
        for (int i = 0; i < num; i++) {
            // Prime
            res.add(new AtomicInteger());
        }
        if (list == null || list.isEmpty()) {
            return transformToJR(res);
        }
        long start = Long.valueOf("" + list.get(0).get("updated")).longValue();
        long end = Long.valueOf("" + list.get(list.size() - 1).get("updated")).longValue();
        long subrange = (end - start) / num;
        if (subrange < 1) {
            subrange = 1;
        }
        long mod = (end - start) / subrange;
        if (mod > 0) {
            log.trace("Having a rest which I need to retain. Just adding it to subrange. Rest is " + mod);
            subrange += mod;
        }
        int count = 0;
        for (int i = 0; i < num; i++) {
            start += subrange;
            //log.debug("Starting at "+start+" at iteration "+i+" having subrange "+subrange);
            // Still having more data, and current updated value less than where to start in scale
            while (count < list.size() && Long.valueOf("" + list.get(count).get("updated")).longValue() <= start) {
                //log.debug("Shall increment place "+i);
                res.get(i).getAndAdd(Integer.valueOf("" + list.get(count).get("counted")).intValue());
                count++;
            }
        }
        if (count < list.size()) {
            log.error("Sanity: Did not use all data!? This may happen if the data are not sorted in the updated field. Missing " + (list.size() - count) + " elements");
        }

        return transformToJR(res);
    }

    private List<JsonResults> transformToJR(List<AtomicInteger> res) {
        List<JsonResults> list = new ArrayList<JsonResults>();
        for (int i = 0; i < res.size(); i++) {
            JsonResults jr = new JsonResults();
            jr.setKey("" + i);
            jr.setValue(res.get(i).toString());
            list.add(jr);
        }
        return list;
    }

    private String leaseInfo(SemiEventRegistration lease) {
        return "{id: " + lease.getId() + ", lease.holderId:" + lease.getLease().getHolderId() + "}";
    }

    /**
     * This method is intended to be used from a junit tests.
     *
     * @param whenStartedTest From inclusive when to delete item
     * @param path            Path to delete, with percentage signs as applicable.
     */
    protected void deleteItemsFrom(long whenStartedTest, String path) {
        rwl.writeLock().lock();
        try {
            jdbcTemplate.update(
                    "DELETE FROM meter WHERE path like ? and updated>=?",
                    new Object[]{path, whenStartedTest});
        } catch (Exception e) {
            throw new RuntimeException("Could not delete items. From " + whenStartedTest + ", " +
                    "path: " + path, e);
        } finally {
            rwl.writeLock().unlock();
        }

    }

    /**
     * @param start Where to start, inclusive
     * @param end   Where to end, inclusive
     */
    public void collate(long start, long end) {
        rwl.readLock().lock();
        // Find new elements
        List<Map<String, Object>> items = null;
        try {
            final String sql = "SELECT path AS path, sum(counted) AS counted, min(updated) AS updated FROM meter " +
                    "WHERE " +
                    "updated>=? AND updated<=? GROUP BY path";
            log.debug("Querying with (" + start + "," + end + ") : " + sql);
            items = jdbcTemplate.queryForList(sql,
                    new Object[]{Long.valueOf(start), Long.valueOf(end)});
        } finally {
            rwl.readLock().unlock();
        }
        // Remove old items
        rwl.writeLock().lock();
        try {
            for (Map<String, Object> item : items) {
                jdbcTemplate.update(
                        "DELETE FROM meter WHERE path like ? AND updated>=? AND updated<=?",
                        new Object[]{item.get("path"), start, end});
            }
        } finally {
            rwl.writeLock().unlock();
        }
        // Translate
        List<Item> replacements = new ArrayList<Item>();
        for (Map<String, Object> item : items) {
            Item i = new Item();
            i.setPath(item.get("path").toString());
            i.setAccessNumber(Integer.parseInt(item.get("counted").toString()));
            i.setWhen(Long.parseLong(item.get("updated").toString()));
            replacements.add(i);
        }
        // Insert
        performInsertion(replacements);
    }
}
