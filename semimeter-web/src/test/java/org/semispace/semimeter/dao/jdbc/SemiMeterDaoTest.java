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

package org.semispace.semimeter.dao.jdbc;

import org.junit.Assert;
import org.junit.Test;
import org.semispace.semimeter.bean.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.ArrayList;
import java.util.List;


@ContextConfiguration(locations = {"/context/semimeter-test-context.xml"})
public class SemiMeterDaoTest extends AbstractJUnit4SpringContextTests {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDaoTest.class);
    private static final long NUMBER_OF_TEST_ELEMENTS = 100;

    @Autowired
    @Qualifier("testDao")
    private SemiMeterDaoImpl semiMeterDao;

    @Test
    public void testPresentDao() {
        Assert.assertNotNull(semiMeterDao);
    }

    @Test
    public void testStandardDaoFunctionality() {
        Assert.assertTrue(semiMeterDao.isAlive());
        int oldSize = semiMeterDao.size();

        Item item = createItem(System.currentTimeMillis());
        List<Item> items = new ArrayList<Item>();
        items.add(item);
        semiMeterDao.performInsertion(items);
        Assert.assertEquals("Should manage to add a single item", oldSize + 1, semiMeterDao.size());
    }

    @Test
    public void testInsertionOfANumberOfItemsInArray() {
        int oldSize = semiMeterDao.size();
        List<Item> items = new ArrayList<Item>();
        for (long x = 1; x < NUMBER_OF_TEST_ELEMENTS; x++) {
            Item item = createItem(x);
            items.add(item);
        }
        long bench = System.currentTimeMillis();
        semiMeterDao.performInsertion(items);
        log.info("Used {} milliseconds on insertion of {} items ", (System.currentTimeMillis() - bench), items.size());
        Assert.assertEquals(oldSize + (NUMBER_OF_TEST_ELEMENTS - 1), semiMeterDao.size());
    }

    @Test
    public void testSingleInsertionOfItemsInArray() {
        int oldSize = semiMeterDao.size();
        long bench = System.currentTimeMillis();
        for (long x = NUMBER_OF_TEST_ELEMENTS; x < 2 * NUMBER_OF_TEST_ELEMENTS; x++) {
            List<Item> items = new ArrayList<Item>();
            Item item = createItem(x);
            items.add(item);
            semiMeterDao.performInsertion(items);
        }
        log.info("Used {} milliseconds in testSingleInsertionOfItemsInArray", (System.currentTimeMillis() - bench));
        Assert.assertEquals(oldSize + (NUMBER_OF_TEST_ELEMENTS), semiMeterDao.size());
    }

    @Test
    public void testSingleInsertionOfItems() {
        int oldSize = semiMeterDao.size();
        long bench = System.currentTimeMillis();
        for (long x = NUMBER_OF_TEST_ELEMENTS * 2; x < 3 * NUMBER_OF_TEST_ELEMENTS; x++) {
            Item item = createItem(x);
            semiMeterDao.insert(item);
        }
        log.info("Used {} milliseconds in testSingleInsertionOfItems", (System.currentTimeMillis() - bench));
        Assert.assertEquals(oldSize + (NUMBER_OF_TEST_ELEMENTS), semiMeterDao.size());
    }


    private Item createItem(long when) {
        Item item = new Item();
        item.setPath("/junit/testStandardDaoFunctionality");
        item.setWhen(when);
        item.increment();
        return item;
    }
}
