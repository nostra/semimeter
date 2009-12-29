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
        /*if ( getSpace().readIfExists(new TakeInProgressMarker()) != null ) {
            return;
        }*/

        CounterHolder ch;
        Collection<Item> items = new ArrayList<Item>();

        /*
        ch = getSpace().readIfExists(new CounterHolder());
        if (ch != null) {
            // We have not taken object yet.
            try {
                // Add a marker which indicates that we are in progress
                getSpace().write(new TakeInProgressMarker(), SemiSpace.ONE_DAY);
                */
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
/*
            } finally {
                // Clear marker
                getSpace().takeIfExists(new TakeInProgressMarker());
            }

        }*/
    }

}
