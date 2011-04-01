package org.semispace.semimeter.space.listener;

import org.semispace.SemiEventListener;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.semimeter.bean.TruncateTimeout;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to expire events for TruncateTimeout beans and truncates data to 24h.
 */
public class TruncateListener implements SemiEventListener {
    private static final Logger log = LoggerFactory.getLogger(TruncateListener.class);
    private SemiSpaceInterface space;
    private SemiMeterDao semiMeterDao;
    private String eventType;

    private static final long expirationTime = 1000 * 60 * 15; //15 min


    private TruncateTimeout template = new TruncateTimeout();

    public TruncateListener(final SemiSpaceInterface space, final SemiMeterDao semiMeterDao, final String eventType) {
        this.space = space;
        this.semiMeterDao = semiMeterDao;
        this.eventType = eventType;
    }


    @Override
    public void notify(final SemiEvent theEvent) {
        if (theEvent instanceof SemiExpirationEvent) {
            log.debug("got event");
            space.takeIfExists(template);
            this.triggerTimeout();
            this.truncate();
        }
    }

    private void truncate() {
        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 60 * 24); //24h
    }

    public void triggerTimeout() {
        //if (space.readIfExists(template) == null) {
            log.debug("setting new timeout bean");
            space.write(new TruncateTimeout(), expirationTime);
        //}
    }
}
