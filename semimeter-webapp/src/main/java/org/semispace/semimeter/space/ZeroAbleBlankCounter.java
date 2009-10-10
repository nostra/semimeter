package org.semispace.semimeter.space;

import org.semispace.SemiSpace;
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
    
    public ZeroAbleBlankCounter( SemiSpaceInterface space ) {
        this.space = space;
    }


    public void reset() {
        rwl.writeLock().lock();
        CounterHolder old = holder;
        holder = new CounterHolder();
        rwl.writeLock().unlock();

        if ( old.size() > 0 ) {
            // Insert lock into space to be counted by separate process.
            space.write(old, SemiSpace.ONE_DAY);
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
