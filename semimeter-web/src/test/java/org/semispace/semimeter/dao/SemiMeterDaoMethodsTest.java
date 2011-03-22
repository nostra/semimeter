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

import org.junit.Assert;
import org.junit.Test;
import org.semispace.semimeter.bean.JsonResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemiMeterDaoMethodsTest {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDaoMethodsTest.class);

    @Test
    public void testEmptySingleEntry() {
        List<JsonResults> jrs = new SemiMeterDao().flatten(new ArrayList<Map<String, Object>>(), 10);
        Assert.assertEquals(10, jrs.size());
        for (JsonResults jr : jrs) {
            Assert.assertEquals("0", jr.getValue());
        }
    }

    @Test
    public void testFlattenSingleEntry() {
        ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        data.add(createMap(1255795233251l, 100));
        List<JsonResults> jrs = new SemiMeterDao().flatten(data, 5);
        Assert.assertEquals(5, jrs.size());
        Assert.assertEquals("100", jrs.get(0).getValue());
    }

    @Test
    public void testFlattenSeveralEntries() {
        ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        data.add(createMap(100, 1));
        data.add(createMap(200, 1));
        data.add(createMap(300, 1));
        data.add(createMap(400, 1));
        data.add(createMap(500, 1));
        List<JsonResults> jrs = new SemiMeterDao().flatten(data, 5);

        for (JsonResults jr : jrs) {
            Assert.assertEquals("The distribution of 5 data should be flat", "1", jr.getValue());
        }
    }

    @Test
    public void testFlattenWhenAllInSameSpot() {
        ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        data.add(createMap(100, 1));
        data.add(createMap(210, 1));
        data.add(createMap(220, 1));
        data.add(createMap(230, 1));
        data.add(createMap(500, 1));
        List<JsonResults> jrs = new SemiMeterDao().flatten(data, 5);

        Assert.assertEquals("1", jrs.get(0).getValue());
        Assert.assertEquals("3", jrs.get(1).getValue());
        Assert.assertEquals("0", jrs.get(2).getValue());
        Assert.assertEquals("1", jrs.get(4).getValue());
    }

    private Map<String, Object> createMap(long updt, int count) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("updated", Long.valueOf(updt));
        map.put("counted", Integer.valueOf(count));
        return map;
    }
}
