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

import org.semispace.SemiEventListener;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.semimeter.bean.ThrottleBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSpace2Dao implements SemiEventListener {
    private final Logger log = LoggerFactory.getLogger(AbstractSpace2Dao.class);
    private SemiSpaceInterface space;
    private SemiMeterDao meterDao;
    private boolean isActive;
    private String eventType;

    public SemiSpaceInterface getSpace() {
        return space;
    }

    public SemiMeterDao getMeterDao() {
        return meterDao;
    }

    public AbstractSpace2Dao(SemiSpaceInterface space, SemiMeterDao meterDao, String eventType) {
        this.space = space;
        this.meterDao = meterDao;
        this.eventType = eventType;
        this.isActive = false;
    }

    @Override
    public void notify(SemiEvent theEvent) {
        if (theEvent instanceof SemiAvailabilityEvent) {
            //log.debug("Got availability in "+toString()+" with id "+theEvent.getId());
            activate();
        } else if (theEvent instanceof SemiExpirationEvent) {
            log.warn("Lost event when listening for: {}. Element had id: {}. Sending message to throttle.", eventType, theEvent.getId());
            getSpace().write(new ThrottleBean(1), 5000);
        }
    }

    public void activate() {
        if (isActive) {
            // Already at it.
            return;
        }
        isActive = true;
        try {
            retrieveAndTreatData();
        } finally {
            isActive = false;
        }

    }

    public abstract void retrieveAndTreatData();

}
