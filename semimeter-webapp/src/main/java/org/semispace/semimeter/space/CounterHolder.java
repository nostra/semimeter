package org.semispace.semimeter.space;

import org.semispace.semimeter.bean.Item;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

/**
 * Holder for counted elements. A premiss for using this class
 * is that it is accessed in a synchronized manner.
 * It is also presumed that this element has a second in resolution.
 */
public class CounterHolder {
    private Map<String, Item> items = new HashMap<String, Item>();
    public void count( String path ) {
        Item item = items.get(path);
        if ( item == null ) {
            item = new Item();
            item.setWhen(System.currentTimeMillis());
            item.setPath(path);
            items.put(path, item);
        }
        item.increment();
    }

    public Collection<Item> retrieveItems() {
        return items.values();
    }
    public int size() {
        return items.size();
    }
}
