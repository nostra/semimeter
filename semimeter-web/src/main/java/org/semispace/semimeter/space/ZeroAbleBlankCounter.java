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

import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Count the different access paths, and be able to reset them. When reset occurs, just
 * insert the holder of items into SemiSpace - leaving further treatment to a listener in
 * the space.
 */
public class ZeroAbleBlankCounter {
    private static Logger log = LoggerFactory.getLogger(ZeroAbleBlankCounter.class);
    private SemiSpaceInterface space;
    private CounterHolder holder = new CounterHolder();
    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    private static final int REGISTRATION_TIMEOUT = 10 * 60 * 1000;

    public ZeroAbleBlankCounter(SemiSpaceInterface space) {
        this.space = space;
        if (space == null) {
            throw new RuntimeException("Not expecting space to be null");
        }
    }


    public void reset() {
        rwl.writeLock().lock();
        CounterHolder old = holder;
        holder = new CounterHolder();
        rwl.writeLock().unlock();

        if (old.size() > 0) {
            // Insert lock into space to be counted by separate process.
            // Timeout 10 minutes - if not registered by then, just forget about them.
            space.write(old, REGISTRATION_TIMEOUT);
        } else {
            log.debug("No data was contained in counter holder.");
        }
    }

    public void count(String path) {
        rwl.writeLock().lock();
        try {
            holder.count(path);
        } finally {
            rwl.writeLock().unlock();
        }
    }
}
