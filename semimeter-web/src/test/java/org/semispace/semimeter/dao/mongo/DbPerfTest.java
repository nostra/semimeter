package org.semispace.semimeter.dao.mongo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/context/mongo-test.context.xml"})
public class DbPerfTest {
    private static final Logger log = LoggerFactory.getLogger(DbPerfTest.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SemiMeterDao semiMeterDao;

    @Before
    public void setUp() {
        mongoTemplate.getCollection("meter").drop();
        mongoTemplate.getCollection("sums").drop();

    }

    /**
     * Original 0.13 branch: 7.21
     * 8m 10s: No writeconcern set
     * 7m 53s: Write concern: NONE
     * 6m 56s: Normal
     * 7m 16s: WriteConcern.UNACKNOWLEDGED
     * 6m 53s: WriteConcern.UNACKNOWLEDGED (set in DAO)
     */
    @Test
    public void insertion_test() {
        long start = System.currentTimeMillis();
        long last = start;

        for ( int i=0 ; i < 50000 ; i++ ) {
            semiMeterDao.performInsertion(
                    Arrays.asList(new Item[]{new Item(start - 1000 * 60 * 60 * 23, "/article/95/37/"+i, 1)}));
            if ( i % 1000 == 0 ) {
                log.debug("* Mark "+i+" * diff "+(System.currentTimeMillis()-last)+" Time spent so far: "+(System.currentTimeMillis()-start));
                last = System.currentTimeMillis();
            }
        }

    }
}
