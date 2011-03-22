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

import org.semispace.semimeter.bean.Item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Holder for counted elements. A premise for using this class
 * is that it is accessed in a synchronized manner.
 * It is also presumed that this element has a second in resolution.
 */
public class CounterHolder {
    private Map<String, Item> items = new HashMap<String, Item>();
    public static final String RESOLUTION_MS_SYSTEM_VARIABLE = "semimeter.frequency.ms";

    /**
     * Consider using a lock instead
     */
    public synchronized void count(String path) {
        Item item = items.get(path);
        if (item == null) {
            item = new Item();
            long when = System.currentTimeMillis();
            item.setWhen(when);
            item.setPath(path);
            items.put(path, item);
        }
        item.increment();
    }

    public synchronized Collection<Item> retrieveItems() {
        return items.values();
    }

    public synchronized int size() {
        return items.size();
    }

    public synchronized String toString() {
        return "[CounterHolder - size " + size() + "]";
    }
}
