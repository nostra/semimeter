package org.semispace.semimeter.dao;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.space.CounterHolder;
import org.semispace.semimeter.space.ZeroAbleBlankCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 *
 */
@ContextConfiguration(locations={"/context/semimeter-test-context.xml"})
public class SemiMeterSpaceConnectionTest extends AbstractJUnit4SpringContextTests {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterSpaceConnectionTest.class);
    private static final long NUMBER_OF_TEST_ELEMENTS = 100;
    private final SemiSpaceInterface space = SemiSpace.retrieveSpace();

    @Autowired
    private SemiMeterDao semiMeterDao;

    private ZeroAbleBlankCounter counter = null;

    @Before
    public void fetchSpaceFromDao() {
        //log.info("Before: Current state of semiMeterDao:\n"+semiMeterDao);
        if ( counter == null ) {
            counter = new ZeroAbleBlankCounter(space);
        }
    }
    @After
    public void status() {
        //log.info("After: Current state of semiMeterDao:\n"+semiMeterDao);
    }

    @Test
    public void testInsertionBySpace() {
        int oldSize = semiMeterDao.size();
        counter.count("/junit/InsertionBySpace");
        counter.reset();
        awaitNoCounterHoldersInSpaceAndSilentDb();
        Assert.assertEquals("Expected size of database to increase after addition of a single element.", oldSize+1, semiMeterDao.size() );
    }

    @Test
    public void testMultipleInsertionBySpace() {
        long start= System.currentTimeMillis();
        for ( int i=0 ; i < NUMBER_OF_TEST_ELEMENTS ; i++ ) {
            counter.count("/junit/InsertionBySpace/"+i);
            counter.reset();
        }
        log.info("After {} ms, {} elements has been put into space", System.currentTimeMillis() - start, NUMBER_OF_TEST_ELEMENTS);
        long bench = System.currentTimeMillis();
        awaitNoCounterHoldersInSpaceAndSilentDb();
        log.info("After {} _more_ ms all elements are found to be put into database. (This number is misleading if the number of elements are few.)", System.currentTimeMillis() - bench);

        // The -1 is as the method works on elements _larger_ than start
        Long count = semiMeterDao.sumItems(start-1, System.currentTimeMillis(), "/junit/InsertionBySpace/%");
        Assert.assertEquals("Presuming all inserted elements to be present", NUMBER_OF_TEST_ELEMENTS, count.longValue());
    }

    @Test
    public void testMultipleInsertionBySpaceWithEqualElements() {
        long start = System.currentTimeMillis();
        for ( int i=0 ; i < NUMBER_OF_TEST_ELEMENTS ; i++ ) {
            counter.count("/junit/InsertionBySpace/count/up");
            counter.reset();
        }
        log.info("After {} ms, {} collapsible elements has been put into space", System.currentTimeMillis() - start, NUMBER_OF_TEST_ELEMENTS);
        long bench = System.currentTimeMillis();
        awaitNoCounterHoldersInSpaceAndSilentDb();
        log.info("After {} _more_ ms all collapsible elements are found to be put into database. (This number is misleading if the number of elements are few.)", System.currentTimeMillis() - bench);

        Long count = semiMeterDao.sumItems(start-1, System.currentTimeMillis(), "/junit/InsertionBySpace/count/up");
        Assert.assertEquals("Presuming all inserted elements to be present", NUMBER_OF_TEST_ELEMENTS, count.longValue());
    }

    private void awaitNoCounterHoldersInSpaceAndSilentDb() {
        int c = 0;
        int numberOfFails=0;
        boolean dbbusy;
        do {
            int dbsize = semiMeterDao.size();
            c++;
            if ( c > 50 ) {
                c=0;
                numberOfFails++;
                log.info("Waiting for space to be empty when querying for counterHolder...");
            }
            if ( numberOfFails > 10 ) {
                CounterHolder ch = space.takeIfExists( new CounterHolder());
                log.info("After taking "+(ch==null?"no":"one")+" element, there is "+(
                        ( space.readIfExists( new CounterHolder()) == null )?"no":"more")+
                        " elements left. semiMeterDao is now: \n"+semiMeterDao);
                Assert.fail("Serious problem with space. Had counter holder which did not get consumed. Taking it now: "+ ch );
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
            dbbusy = semiMeterDao.size() != dbsize;
        } while ( dbbusy || space.readIfExists( new CounterHolder()) != null );
    }
}
