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

import org.semispace.SemiEventListener;
import org.semispace.SemiSpace;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.semimeter.space.CounterHolder;
import org.semispace.semimeter.bean.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Collection;

@Service("semimeterDao")
public class SemiMeterDao implements InitializingBean, DisposableBean, SemiEventListener {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDao.class);

    private SimpleJdbcTemplate jdbcTemplate;
    private SemiEventRegistration registration;
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

    public Long maxId() {
        long result = 0;
        rwl.readLock().lock();
        try {
            result = jdbcTemplate.queryForLong("select max(id) from meter");
        } catch (DataAccessException e) {
            log.warn("Table probably not yet created. Got (intentionally masked) "+e.getMessage());
        } finally {
            rwl.readLock().unlock();
        }
        return result;
    }

    /**
     * Method called from Spring. Will (try to) create the table with the meter, if it does not already exist.
     */
    public void afterPropertiesSet() {
        if ( registration != null ) {
            log.error("Did not expect SemiSpace registration to exist already. Not registering again");
        } else {
            // Listen for events a year
            registration = space.notify(new CounterHolder(), this, SemiSpace.ONE_DAY*3650);
        }

        if ( size() >= 0 ) {
            return;
        }
        log.info("Creating table stalegroup");
        // The data type integer in the database is a long in the java world.
        jdbcTemplate.getJdbcOperations().execute("create table meter(id integer PRIMARY KEY, updated bigint NOT NULL, count integer NOT NULL, path varchar("+MAX_PATH_LENGTH+") NOT NULL)");
        try {
            jdbcTemplate.getJdbcOperations().execute("create index meter_updt_ix on stalegroup( updated )");
            jdbcTemplate.getJdbcOperations().execute("create index meter_path_ix on stalegroup( path )");
        } catch ( Exception e ) {
            log.error("Did not manage to create index on updated field. Ignoring this, as this probably occured in a " +
                    "junit test, and not in the live system. Masked exception: "+e);
        }
    }

    @Override
    public void notify(SemiEvent theEvent) {
        if ( theEvent instanceof SemiAvailabilityEvent ) {
            retrieveAndTreatItemData();
        }
    }

    private void retrieveAndTreatItemData() {
        CounterHolder ch;
        do {
            ch = space.takeIfExists(new CounterHolder());
            if ( ch != null ) {
                performInsertion( ch.retrieveItems());
            }
        } while ( ch != null);
    }

    private void performInsertion(Collection<Item> items) {
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
            // First figure out whether the entry already is present
            Long same = Long.valueOf(jdbcTemplate.queryForLong("select max(id) from meter " +
                    "WHERE " +
                    "updated=? AND path=?",
                    new Object[]{item.getWhen(), item.getPath()}));
            if ( same == null || same.longValue() == 0 ) {
                // Insert element
                try {
                    jdbcTemplate.update("insert into meter(id, updated, count, path) values (?, ?, 0, ?)",
                        new Object[] {maxId()+1, item.getWhen(), item.getPath()});
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
        if ( registration != null ) {
            registration.getLease().cancel();
            registration = null;
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
}
