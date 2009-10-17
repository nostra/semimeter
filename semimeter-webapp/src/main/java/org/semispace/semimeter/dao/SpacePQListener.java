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

import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.ParameterizedQuery;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.ParameterizedQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpacePQListener extends AbstractSpace2Dao {
    private final Logger log = LoggerFactory.getLogger(SpacePQListener.class);
    /**
     * Max query life 50 sec
     */
    private static final long MAX_RESULT_LIFE_MS = 50000;

    public SpacePQListener(SemiSpaceInterface space, SemiMeterDao meterDao) {
        super(space, meterDao);
    }

    @Override
    public void retrieveAndTreatData() {
        ParameterizedQuery pq;
        do {
            log.debug("Taking PQ");
            pq = getSpace().takeIfExists(new ParameterizedQuery());
            if ( pq != null ) {
                log.debug("Found PQ with key "+pq.getKey());
                if ( getSpace().readIfExists(new ParameterizedQueryResult(pq.getKey(), null)) != null) {
                    log.debug("Query already performed - not doing it again.");
                } else {
                    JsonResults[] result = getMeterDao().performParameterizedQuery(pq.getStartAt(), pq.getEndAt(), pq.getPath());
                    ParameterizedQueryResult pqr = new ParameterizedQueryResult(pq.getKey(), result);
                    long life = (pq.getEndAt() - pq.getStartAt() / 2);
                    if ( life > MAX_RESULT_LIFE_MS) {
                        life = MAX_RESULT_LIFE_MS;
                    }
                    getSpace().write(pqr, MAX_RESULT_LIFE_MS);
                }
            } else {
                log.debug("No PQ found.");
            }
        } while ( pq != null);

    }
}

