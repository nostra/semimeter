package org.semispace.semimeter.space.listener;

import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.semispace.semimeter.space.CounterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Read from space in order to get elements to insert into database.
 * This connection ensures that we do not have too many simultaneous insertions.
 */
public class Space2Dao extends AbstractSpace2Dao {
    private static final Logger log = LoggerFactory.getLogger(Space2Dao.class);

    public Space2Dao(SemiSpaceInterface space, SemiMeterDao meterDao, String eventType) {
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
