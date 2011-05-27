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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.semispace.semimeter.bean.GroupedResult;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.TokenizedPathInfo;
import org.semispace.semimeter.bean.mongo.MeterHit;
import org.semispace.semimeter.bean.mongo.PathElements;
import org.semispace.semimeter.dao.AbstractSemiMeterDaoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository("semimeterDao")
public class SemiMeterDaoMongo2 extends AbstractSemiMeterDaoImpl {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDaoMongo2.class);

    @Autowired
    MongoTemplate mongoTemplate;

    private ReadWriteLock rwl = new ReentrantReadWriteLock();


    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void performInsertion(final Collection<Item> items) {
        for (Item item : items) {
            //some time calculations
            Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(item.getWhen());
            cal.set(Calendar.MILLISECOND, 0);
            long second = cal.getTimeInMillis();
            cal.set(Calendar.SECOND, 0);
            long minute = cal.getTimeInMillis();
            cal.set(Calendar.MINUTE, 0);
            long hour = cal.getTimeInMillis();

            BasicDBObject query = new BasicDBObject();
            PathElements pathElements = MeterHit.calcPath(item.getPath(), "/");
            query.append("id", Integer.valueOf(pathElements.getE4()).intValue());
            query.append("sectionId", Integer.valueOf(pathElements.getE3()).intValue());
            query.append("publicationId", Integer.valueOf(pathElements.getE2()).intValue());
            query.append("type", pathElements.getE1());

            StringBuilder sb = new StringBuilder();
            sb.append(" { '$inc': ");
            sb.append("      { 'day.count' : " + item.getAccessNumber() + ", ");
            sb.append("        'day.hours." + hour + ".count' : " + item.getAccessNumber() + ",  ");
            sb.append("        'day.hours." + hour + ".minutes." + minute + ".count' : " + item.getAccessNumber() +
                    ",  ");
            sb.append("        'day.hours." + hour + ".minutes." + minute + ".seconds." + second + ".count' : " +
                    item.getAccessNumber() + "  ");
            sb.append(" } }");

            DBObject update = (DBObject) JSON.parse(sb.toString());

            mongoTemplate.getDefaultCollection().update(query, update, true, false);
        }
    }

    @Override
    public Long sumItems(final long startAt, final long endAt, final String path) {
        return null;
    }

    @Override
    public JsonResults[] performParameterizedQuery(final long startAt, final long endAt, final String path) {
        return new JsonResults[0];
    }

    @Override
    public JsonResults[] createTimeArray(final String path, final long endAt, final long startAt,
            final Integer numberOfSamples) {
        return new JsonResults[0];
    }

    @Override
    public List<GroupedResult> getGroupedSums(final long startAt, final long endAt, final TokenizedPathInfo query,
            final int maxResults) throws IllegalArgumentException {
        return null;
    }

    @Override
    public List<GroupedResult> getHourlySums(final Integer publicationId, final Integer sectionId) {
        return null;
    }

    @Override
    public void deleteEntriesOlderThanMillis(final long millis) {
    }
}
