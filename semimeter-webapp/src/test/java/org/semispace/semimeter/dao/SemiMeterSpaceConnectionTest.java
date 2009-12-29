package org.semispace.semimeter.dao;

import org.junit.Assert;
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
    private SemiSpaceInterface space = SemiSpace.retrieveSpace();

    @Autowired
    private SemiMeterDao semiMeterDao;

    private ZeroAbleBlankCounter counter = new ZeroAbleBlankCounter(space);

    @Test
    public void testInsertionBySpace() {
        int oldSize = semiMeterDao.size();
        counter.count("/junit/InsertionBySpace");
        counter.reset();
        awaitNoCounterHoldersInSpace();
        Assert.assertEquals("Expected size of database to increase after addition of a single element.", oldSize+1, semiMeterDao.size() );
    }

    @Test
    public void testMultipleInsertionBySpace() {
        long bench = System.currentTimeMillis();
        for ( int i=0 ; i < NUMBER_OF_TEST_ELEMENTS ; i++ ) {
            counter.count("/junit/InsertionBySpace/"+i);
            counter.reset();
        }
        log.info("After {} ms, {} elements has been put into space", System.currentTimeMillis() - bench, NUMBER_OF_TEST_ELEMENTS);
        bench = System.currentTimeMillis();
        awaitNoCounterHoldersInSpace();
        log.info("After {} _more_ ms all elements are found to be put into database. (This number is misleading if the number of elements are few.)", System.currentTimeMillis() - bench);
    }

    private void awaitNoCounterHoldersInSpace() {
        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        } while ( space.readIfExists( new CounterHolder()) != null );
    }
}
