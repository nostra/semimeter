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

package org.semispace.semimeter.dao.mongo;

import com.mongodb.DB;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service("semimeterDao")
public class SemiMeterDaoImpl implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDaoImpl.class);

    @Autowired
    @Qualifier("db")
    private DB db;
    private SemiEventRegistration chRegistration;
    private SemiEventRegistration pqRegistration;
    private SemiEventRegistration aqRegistration;

    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    private SemiSpaceInterface space;


    /**
     * @return size of stalegroup table, or -1 if any errors occur.
     */
    public int size() {
        int result = -1;
        rwl.readLock().lock();
        try {
            result = (int) db.getCollection("meter").count();
        } finally {
            rwl.readLock().unlock();
        }

        return result;
    }

    public boolean isAlive() {
        rwl.readLock().lock();
        try {
            int result = (int) db.getCollection("meter").count();
            if (result >= 0) {
                return true;
            }
        } finally {
            rwl.readLock().unlock();
        }
        return false;
    }
}
