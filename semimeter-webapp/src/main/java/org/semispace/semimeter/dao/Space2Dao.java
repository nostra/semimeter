package org.semispace.semimeter.dao;

import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.ThrottleBean;
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
    /**
     * Throttle if more than 10 elements in queue.
     */
    private static final int THROTTLE_THRESHOLD = 10;

    public Space2Dao(SemiSpaceInterface space, SemiMeterDao meterDao, String eventType ) {
        super(space, meterDao, eventType);
    }

    public void retrieveAndTreatData() {
        CounterHolder ch;
        Collection<Item> items = new ArrayList<Item>();
        int numberOfTimesInLoop = 0;
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
                numberOfTimesInLoop++;
                if ( numberOfTimesInLoop % THROTTLE_THRESHOLD == 0 ) {
                    getSpace().write(new ThrottleBean( 1 ), 5000 );
                }
                
            }
        } while (ch != null);
        if ( numberOfTimesInLoop == 1 ) {
            // Throttle down
            getSpace().write(new ThrottleBean( -1 ), 60*1000 );
        }
    }

}
