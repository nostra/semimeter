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
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.ParameterizedQuery;
import org.semispace.semimeter.space.CounterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service("semimeterDao")
public class SemiMeterDao implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDao.class);

    private SimpleJdbcTemplate jdbcTemplate;
    private SemiEventRegistration chRegistration;
    private SemiEventRegistration pqRegistration;
    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    private SemiSpaceInterface space = SemiSpace.retrieveSpace();
    private static final int MAX_PATH_LENGTH = 2048;

    @Autowired
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
            log.warn("Table probably not yet created. Got (intentionally masked) "+e.getMessage());
        } finally {
            rwl.readLock().unlock();
        }

        return result;
    }

    public boolean isAlive() {
        rwl.readLock().lock();
        try {
            int result = this.jdbcTemplate.queryForInt("select count(*) from meter where updated < 1");
            if ( result >= 0 ) {
                return true;
            }
        } catch (DataAccessException e) {
            log.warn("Table probably not yet created. Got (intentionally masked) "+e.getMessage());
        } finally {
            rwl.readLock().unlock();
        }
        return false;
    }

    /**
     * Method called from Spring. Will (try to) create the table with the meter, if it does not already exist.
     */
    public void afterPropertiesSet() {
        log.debug("Registering listeners.");
        if ( chRegistration != null || pqRegistration != null) {
            log.error("Did not expect any SemiSpace registration to exist already. Not registering again");
        } else {
            // Listen for events a year
            chRegistration = space.notify(new CounterHolder(), new Space2Dao( space, this), SemiSpace.ONE_DAY*3650);
            pqRegistration = space.notify(new ParameterizedQuery(), new SpacePQListener( space, this), SemiSpace.ONE_DAY*3650);
        }

        if ( size() >= 0 ) {
            return;
        }
        log.info("Creating table stalegroup");
        // The data type integer in the database is a long in the java world.
        jdbcTemplate.getJdbcOperations().execute("create table meter(updated bigint NOT NULL, count integer NOT NULL, path varchar("+MAX_PATH_LENGTH+") NOT NULL)");
        try {
            jdbcTemplate.getJdbcOperations().execute("create index meter_updt_ix on stalegroup( updated )");
            jdbcTemplate.getJdbcOperations().execute("create index meter_path_ix on stalegroup( path )");
        } catch ( Exception e ) {
            log.error("Did not manage to create index on updated field. Ignoring this, as this probably occured in a " +
                    "junit test, and not in the live system. Masked exception: "+e);
        }
    }

    protected void performInsertion(Collection<Item> items) {
        for ( Item item : items ) {
            insert( item );
        }
    }

    /**
     * Insertion is performed like this: Try to insert item with count of zero if does not already
     * exist. Then update the count to the correct value. The candidate key for the element is
     * (when + path).
     */
    private void insert(Item item) {
        rwl.writeLock().lock();
        try {
            Long same = null;
            try {
                // First figure out whether the entry already is present - querying on trivial field
                same = Long.valueOf(jdbcTemplate.queryForLong("select updated from meter " +
                        "WHERE " +
                        "updated=? AND path=?",
                        new Object[]{item.getWhen(), item.getPath()}));
            } catch ( EmptyResultDataAccessException erdae ) {
                // Expected and OK
            }
            if ( same == null || same.longValue() == 0 ) {
                // Insert element
                try {
                    jdbcTemplate.update("insert into meter(updated, count, path) values (?, 0, ?)",
                        new Object[] { item.getWhen(), item.getPath()});
                } catch ( Exception e ) {
                    log.error("Got exception inserting element. Non-fatal, but we will probably loose the count of "+item+". Masked exception "+e);
                }
            }

            jdbcTemplate.update(
                    "update meter SET count=count+? WHERE path like ? and updated=?",
                    new Object[] {item.getAccessNumber(), item.getPath(), item.getWhen()});

        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void destroy() throws Exception {
        if ( chRegistration != null ) {
            chRegistration.getLease().cancel();
            chRegistration = null;
        }
        if ( pqRegistration!= null ) {
            pqRegistration.getLease().cancel();
            pqRegistration = null;
        }
    }

    public Long sumItems(long startAt, long endAt, String path) {
        Long result = Long.valueOf(-1);
        rwl.readLock().lock();
        try {
            final String sql = "select sum(count) from meter " +
                    "WHERE " +
                    "updated>? AND updated<=?  AND path like ?";
            //log.debug("Querying with ("+startAt+","+endAt+","+path+") : "+sql);
            Long sum = Long.valueOf(jdbcTemplate.queryForLong(sql,
                    new Object[]{Long.valueOf( startAt ), Long.valueOf( endAt ), path}));
            result = sum;
        } finally {
            rwl.readLock().unlock();
        }

        return result;
    }

    public JsonResults[] performParameterizedQuery(long startAt, long endAt, String path) {
        if ( path.indexOf("$") == -1 || path.indexOf("$") != path.lastIndexOf("$") ) {
            throw new RuntimeException("Expecting one and only one $");
        }
        List<JsonResults> jrs = new ArrayList<JsonResults>();
        rwl.readLock().lock();
        try {
            String prefix = path.substring(0, path.indexOf("$"));
            String postfix = path.substring(path.indexOf("$")+1);
            List<String> variants = createStringListOfVariants(startAt, endAt, path);
            log.trace("Got variants: {}", variants);
            for ( String s : variants.toArray(new String[0]) ) {
                Long sum = sumItems(startAt, endAt, prefix+s+postfix);
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
        List <String> list = new ArrayList<String>();
        rwl.readLock().lock();
        try {
            String prefix = path.substring(0, path.indexOf("$"));
            String postfix = path.substring(path.indexOf("$")+1);
            String sql = "SELECT distinct path AS path FROM meter WHERE path like ? " +
                    "AND path like ? AND updated>? AND updated<=? ORDER BY path";
            List<Map<String,Object>> result = jdbcTemplate.queryForList(sql ,
                    new Object[]{prefix+"%",
                            "%"+postfix+"%",
                            Long.valueOf( startAt ), Long.valueOf( endAt ) });

            //log.debug("Got "+result.size()+" results when doing "+sql+" with regards to ("+prefix+","+postfix+", "+startAt+","+endAt+")");
            for ( Map<String, Object> m : result ) {
                String s = (String) m.get("path");
                s = s.substring(prefix.length(), s.length() - postfix.length());
                list.add( s );
            }
        } finally {
            rwl.readLock().unlock();
        }

        return list;
    }
}
