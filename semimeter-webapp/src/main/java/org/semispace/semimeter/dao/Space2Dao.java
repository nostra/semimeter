package org.semispace.semimeter.dao;

import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.space.CounterHolder;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Read from space in order to get elements to insert into database.
 * This connection ensures that we do not have too many simultaneous insertions.
 */
public class Space2Dao extends AbstractSpace2Dao {
    public Space2Dao(SemiSpaceInterface space, SemiMeterDao meterDao, String eventType ) {
        super(space, meterDao, eventType);
    }

    public void retrieveAndTreatData() {
        CounterHolder ch;
        Collection<Item> items = new ArrayList<Item>();

        do {
            ch = getSpace().takeIfExists(new CounterHolder());
            if (ch == null) {
                if (!items.isEmpty()) {
                    getMeterDao().performInsertion(items);
                    items.clear();
                    // Forcing another loop - things may have been introduced whilst inserting.
                    ch = new CounterHolder();
                }
            } else {
                items.addAll(ch.retrieveItems());
            }
        } while (ch != null);
    }

}
