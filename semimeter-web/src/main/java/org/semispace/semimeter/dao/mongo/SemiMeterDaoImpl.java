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

import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.GroupedResult;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.TokenizedPathInfo;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//@Repository("semimeterDao")
public class SemiMeterDaoImpl implements SemiMeterDao {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDaoImpl.class);

    @Autowired
    MeterRepository meterRepository;

    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    private SemiSpaceInterface space;

    @Override
    public int size() {
        int result = -1;
        rwl.readLock().lock();
        try {
            result = meterRepository.count().intValue();
        } finally {
            rwl.readLock().unlock();
        }

        return result;
    }

    @Override
    public boolean isAlive() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Long sumItems(long startAt, long endAt, String path) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JsonResults[] performParameterizedQuery(long startAt, long endAt, String path) {
        return new JsonResults[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JsonResults[] createTimeArray(String path, long endAt, long startAt, Integer numberOfSamples) {
        return new JsonResults[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<GroupedResult> getGroupedSums(long startAt, long endAt, TokenizedPathInfo query, int maxResults)
            throws IllegalArgumentException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<GroupedResult> getHourlySums(final Integer publicationId, final Integer sectionId) {
        return null;
    }

    @Override
    public void deleteEntriesOlderThanMillis(final long millis) {
    }

    @Override
    public void performInsertion(Collection<Item> items) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
