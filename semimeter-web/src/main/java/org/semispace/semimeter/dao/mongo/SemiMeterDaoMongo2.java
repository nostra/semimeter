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
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository("semimeterDao")
public class SemiMeterDaoMongo2 extends AbstractSemiMeterDaoImpl {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDaoMongo2.class);
    private DateFormat df = new SimpleDateFormat("yy-MM-dd-HH-mm");

    @Autowired
    MongoTemplate mongoTemplate;

    private ReadWriteLock rwl = new ReentrantReadWriteLock();


    @Override
    public int size() {
        return (int) mongoTemplate.getDefaultCollection().count();
    }

    @Override
    public boolean isAlive() {
        return mongoTemplate.getDb().getMongo().getConnector().isOpen();
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
            cal.setTimeInMillis(item.getWhen());

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
            sb.append(
                    "        'day.hours." + hour + ".minutes." + minute + ".count' : " + item.getAccessNumber() + "  ");
            //sb.append("        'day.hours." + hour + ".minutes." + minute + ".seconds." + second + ".count' : " +                    item.getAccessNumber() + "  ");
            sb.append(" } }");

            DBObject update = (DBObject) JSON.parse(sb.toString());

            mongoTemplate.getDefaultCollection().update(query, update, true, false);

            query = new BasicDBObject();
            BasicDBObject time = new BasicDBObject();
            query.append("time", time);
            time.append("ts", minute);
            time.append("year", cal.get(Calendar.YEAR));
            time.append("month", cal.get(Calendar.MONTH));
            time.append("day", cal.get(Calendar.DAY_OF_MONTH));
            time.append("hour", cal.get(Calendar.HOUR_OF_DAY));
            time.append("minute", cal.get(Calendar.MINUTE));

            sb = new StringBuilder();
            sb.append(" { '$inc': ");
            sb.append("{ 'total' : ").append(item.getAccessNumber());
            if (pathElements.getE1().equals("article")) {
                sb.append(", 'article' : ").append(item.getAccessNumber());
            } else if (pathElements.getE1().equals("album")) {
                sb.append(", 'album' : ").append(item.getAccessNumber());
            } else if (pathElements.getE1().equals("video")) {
                sb.append(", 'video' : ").append(item.getAccessNumber());
            } else {
                sb.append(", 'other' : ").append(item.getAccessNumber());
            }
            sb.append(" } }");

            update = (DBObject) JSON.parse(sb.toString());

            mongoTemplate.getCollection("sums").update(query, update, true, false);

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
        List<GroupedResult> result = new ArrayList<GroupedResult>();
        if (publicationId == null) {
            if (sectionId != null) {
                throw new IllegalArgumentException("cant have sectionId without publicationId as parameters.");
            }
            //total network
            DBObject sortObj = (DBObject) JSON.parse("{'time.ts': 1}");
            DBCursor dbResult = mongoTemplate.getCollection("sums").find().sort(sortObj);
            while (dbResult.hasNext()) {
                DBObject sum = dbResult.next();
                GroupedResult groupedResult = new GroupedResult();
                Long ts = (Long) ((DBObject) sum.get("time")).get("ts");
                String time = df.format(new Date(ts));
                groupedResult.setKey(time);
                groupedResult.setCount((Integer) sum.get("total"));
                groupedResult.setKeyName("timestamp");
                groupedResult.getSplitCounts()
                        .put("article", sum.get("article") == null ? 0 : (Integer) sum.get("article"));
                groupedResult.getSplitCounts().put("album", sum.get("album") == null ? 0 : (Integer) sum.get("album"));
                groupedResult.getSplitCounts().put("video", sum.get("video") == null ? 0 : (Integer) sum.get("video"));
                groupedResult.getSplitCounts().put("other", sum.get("other") == null ? 0 : (Integer) sum.get("other"));
                result.add(groupedResult);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("function() { ");
            sb.append("    for (h in this.day.hours) { ");
            sb.append("        var hour = this.day.hours[h]; ");
            sb.append("        for (m in hour.minutes) { ");
            sb.append("            var cnt = hour.minutes[m].count; ");
            sb.append("            var counter = { ");
            sb.append("                'total': cnt, ");
            sb.append("                'article': this.type == 'article' ? cnt : 0, ");
            sb.append("                'album': this.type == 'album' ? cnt : 0,");
            sb.append("                'video': this.type == 'video' ? cnt : 0 ");
            sb.append("            }; ");
            sb.append("            emit(m, counter); ");
            sb.append("        }");
            sb.append("    }");
            sb.append("}");
            String map = sb.toString();

            sb.setLength(0);
            sb.append("function(key, values) { ");
            sb.append("    var result = {      ");
            sb.append("        total: 0, ");
            sb.append("        article: 0,");
            sb.append("        album:0,");
            sb.append("        video:0 ");
            sb.append("    }; ");
            sb.append("    for (v in values) { ");
            sb.append("        var cnt = values[v]; ");
            sb.append("        result.total += cnt.total; ");
            sb.append("        result.article += cnt.article; ");
            sb.append("        result.album += cnt.album; ");
            sb.append("        result.video += cnt.video ");
            sb.append("    }; ");
            sb.append("    return result; ");
            sb.append("};");
            String reduce = sb.toString();
            String outputCollectionName = "sums_" + publicationId + "_" + sectionId;
            DBObject query = null;
            if (sectionId == null) {
                query = new BasicDBObject("publicationId", publicationId);
            } else {
                query = new BasicDBObject("sectionId", sectionId);
            }

            MapReduceCommand command =
                    new MapReduceCommand(mongoTemplate.getDefaultCollection(), map, reduce, outputCollectionName,
                            MapReduceCommand.OutputType.REPLACE, query);

            mongoTemplate.getDefaultCollection().mapReduce(command);

            DBCursor dbResult =
                    mongoTemplate.getCollection(outputCollectionName).find().sort(new BasicDBObject("_id", 1));
            while (dbResult.hasNext()) {
                DBObject sum = dbResult.next();
                System.out.println(sum);
                GroupedResult groupedResult = new GroupedResult();
                Long ts = Long.valueOf((String) sum.get("_id"));
                String time = df.format(new Date(ts));
                groupedResult.setKey(time);
                DBObject value = (DBObject) sum.get("value");
                groupedResult.setCount(((Double) value.get("total")).intValue());
                groupedResult.setKeyName("timestamp");
                groupedResult.getSplitCounts()
                        .put("article", value.get("article") == null ? 0 : ((Double) value.get("article")).intValue());
                groupedResult.getSplitCounts()
                        .put("album", value.get("album") == null ? 0 : ((Double) value.get("album")).intValue());
                groupedResult.getSplitCounts()
                        .put("video", value.get("video") == null ? 0 : ((Double) value.get("video")).intValue());
                groupedResult.getSplitCounts()
                        .put("other", value.get("other") == null ? 0 : ((Double) value.get("other")).intValue());
                result.add(groupedResult);
            }

        }
        return result;
    }

    @Override
    public void deleteEntriesOlderThanMillis(final long millis) {
        long when = System.currentTimeMillis() - millis;
        deleteOldSums(when);
        deleteOldMinutes(when);
    }

    private void deleteOldMinutes(long when) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(when);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        long targetHour = cal.getTimeInMillis();

        DBCursor result = mongoTemplate.getDefaultCollection().find(new BasicDBObject(), new BasicDBObject("_id", 1));
        while (result.hasNext()) {
            DBObject doc = result.next();

            //start a new "session" for each document
            mongoTemplate.getDefaultCollection().getDB().requestStart();

            //and fetch actual object (result only contains _id's)
            doc = (DBObject) mongoTemplate.getDefaultCollection().findOne(doc);

            log.trace("cleaning document : {}", doc);
            DBObject day = (DBObject) doc.get("day");
            //log.trace("day: {}", day);
            DBObject hours = (DBObject) day.get("hours");
            //log.trace("hours: {}", hours);
            Set<String> hrSet = new HashSet<String>();
            hrSet.addAll(hours.keySet());
            boolean docChanged = false;

            if (hrSet.isEmpty()) {
                System.out.println("no hours in document, remove it");
                mongoTemplate.getDefaultCollection().remove(new BasicDBObject("_id", doc.get("_id")));
            } else {

                for (String h : hrSet) {
                    long hourmillis = Long.valueOf(h);
                    log.trace("checking hour: {}", hourmillis);
                    if (hourmillis < targetHour) {
                        if (log.isTraceEnabled()) {
                            log.trace("removing hour " + h + " because it is older than target" + targetHour);
                        }
                        docChanged = true;
                        DBObject obj = (DBObject) hours.get(h);
                        day.put("count", (Integer) day.get("count") - (Integer) obj.get("count"));
                        hours.removeField(h);
                    } else if (hourmillis == targetHour) {
                        log.trace("current hour is targetHour, check minutes");
                        DBObject currentHour = (DBObject) hours.get(h);
                        DBObject minutes = (DBObject) currentHour.get("minutes");
                        Set<String> keys = new HashSet<String>();
                        keys.addAll(minutes.keySet());
                        for (String m : keys) {
                            long minutemillis = Long.valueOf(m);
                            log.trace("checking minute: {}", minutemillis);
                            if (minutemillis < when) {
                                if (log.isTraceEnabled()) {
                                    log.trace("removing minute " + minutemillis + " because it is older than " + when);
                                }

                                docChanged = true;
                                DBObject obj = (DBObject) minutes.get(m);
                                DBObject hourObj = (DBObject) hours.get(h);
                                day.put("count", (Integer) day.get("count") - (Integer) obj.get("count"));
                                hourObj.put("count", (Integer) hourObj.get("count") - (Integer) obj.get("count"));
                                minutes.removeField(m);
                            }
                        }
                        if (minutes.keySet().isEmpty()) {
                            log.trace("no more minutes, removing hour {}", h);
                            hours.removeField(h);
                            docChanged = true;
                        }
                    }
                }
            }
            if (docChanged) {
                mongoTemplate.getDefaultCollection().save(doc);
            }
            mongoTemplate.getDefaultCollection().getDB().requestDone();
        }
    }

    private void deleteOldSums(long when) {
        DBCursor result =
                mongoTemplate.getCollection("sums").find((DBObject) JSON.parse("{'time.ts': {'$lt': " + when + "}}"));

        while (result.hasNext()) {
            mongoTemplate.getCollection("sums").remove(result.next());
        }
    }
}
