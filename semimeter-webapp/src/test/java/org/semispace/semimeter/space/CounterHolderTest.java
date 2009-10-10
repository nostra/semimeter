package org.semispace.semimeter.space;

import org.junit.Assert;
import org.junit.Test;
import org.semispace.semimeter.bean.Item;

import java.util.Collection;

public class CounterHolderTest {
    @Test
    public void testCount() {
        CounterHolder ch = new CounterHolder();
        Assert.assertEquals(0, ch.size());
        ch.count("/a/b");
        Assert.assertEquals(1, ch.size());
        ch.count("/a/c");
        Assert.assertEquals(2, ch.size());
        ch.count("/a/b");
        Assert.assertEquals(2, ch.size());
        Collection<Item> items = ch.retrieveItems();
        boolean found = false;
        for ( Item item : items ) {
            if ( item.getPath().equals("/a/b")) {
                found = true;
            }
            Assert.assertTrue( item.getWhen() > 0);
            Assert.assertTrue( item.getAccessNumber() > 0);
        }
        Assert.assertTrue( found );
    }
}
