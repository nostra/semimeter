package org.semispace.semimeter.dao;

import org.semispace.SemiEventListener;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.semimeter.space.CounterHolder;

/**
 * Read from space in order to get elements to insert into database.
 * This connection ensures that we do not have too many simultaneous insertions.
 */
public class Space2Dao implements SemiEventListener {
    private SemiSpaceInterface space;
    private SemiMeterDao meterDao;
    private boolean isActive;

    public Space2Dao(SemiSpaceInterface space, SemiMeterDao meterDao ) {
        this.space = space;
        this.meterDao = meterDao;
        this.isActive = false;
    }

    public void activate() {
        if ( isActive ) {
            // Already at it.
            return;
        }
        retrieveAndTreatItemData();
    }
    private void retrieveAndTreatItemData() {
        isActive = true;
        try {
            CounterHolder ch;
            do {
                ch = space.takeIfExists(new CounterHolder());
                if ( ch != null ) {
                    meterDao.performInsertion( ch.retrieveItems());
                }
            } while ( ch != null);
        } finally {
            isActive = false;
        }
    }

    public void notify(SemiEvent theEvent) {
        if ( theEvent instanceof SemiAvailabilityEvent) {
            activate();
        }
    }
}
