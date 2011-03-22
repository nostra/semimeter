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
        for (Item item : items) {
            if (item.getPath().equals("/a/b")) {
                found = true;
            }
            Assert.assertTrue(item.getWhen() > 0);
            Assert.assertTrue(item.getAccessNumber() > 0);
        }
        Assert.assertTrue(found);
    }
}
