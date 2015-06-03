package org.semispace.semimeter.dao.jdbc;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semispace.semimeter.bean.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
@ContextConfiguration(locations = {"/context/semimeter-test-context.xml"})
public class StrategyForReductionTest extends AbstractJUnit4SpringContextTests {
    @Autowired
    @Qualifier("testDao")
    private SemiMeterDaoJdbc semiMeterDao;
    private long whenStartedTest;

    @Before
    public void noteWhen() {
        whenStartedTest = System.currentTimeMillis();
    }

    @After
    public void deleteAddedTestEntries() {
        semiMeterDao.deleteItemsFrom(whenStartedTest, "/junit%");
    }

    @Test
    public void testHousekeeping() {
        int oldSize = semiMeterDao.size();
        Collection<Item> items = new ArrayList<Item>();
        items.add(createItem(whenStartedTest + 0));
        items.add(createItem(whenStartedTest + 1000));
        items.add(createItem(whenStartedTest + 2000));
        semiMeterDao.performInsertion(items);
        Assert.assertEquals(oldSize + 3, semiMeterDao.size());
        semiMeterDao.deleteItemsFrom(whenStartedTest, "/junit%");
        Assert.assertEquals(oldSize, semiMeterDao.size());
    }

    // TODO Does not work with hsql: @Test
    public void testThatCollatingIsPerformed() {
        int oldSize = semiMeterDao.size();

        Collection<Item> items = new ArrayList<Item>();
        items.add(createItem(whenStartedTest + 50000));
        items.add(createItem(whenStartedTest + 51000));
        items.add(createItem(whenStartedTest + 52000));
        items.add(createItem(whenStartedTest + 53000));

        semiMeterDao.performInsertion(items);
        semiMeterDao.collate(whenStartedTest, whenStartedTest + 60000);

        Assert.assertEquals("Having inserted 4 elements, they are collated into a single one",
                oldSize + 1, semiMeterDao.size());
    }

    private Item createItem(long when) {
        Item item = new Item();
        item.setPath("/junit/test/reduction");
        item.setWhen(when);
        item.increment();
        return item;
    }
}
