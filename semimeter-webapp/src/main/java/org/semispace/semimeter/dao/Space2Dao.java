package org.semispace.semimeter.dao;

import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.space.CounterHolder;

/**
 * Read from space in order to get elements to insert into database.
 * This connection ensures that we do not have too many simultaneous insertions.
 */
public class Space2Dao extends AbstractSpace2Dao {
    public Space2Dao(SemiSpaceInterface space, SemiMeterDao meterDao) {
        super(space, meterDao);
    }

    public void retrieveAndTreatData() {
        CounterHolder ch;
        do {
            ch = getSpace().takeIfExists(new CounterHolder());
            if ( ch != null ) {
                getMeterDao().performInsertion( ch.retrieveItems());
            }
        } while ( ch != null);
    }


}
