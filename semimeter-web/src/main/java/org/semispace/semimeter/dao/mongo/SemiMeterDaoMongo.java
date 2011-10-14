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
import com.mongodb.util.JSON;
import org.semispace.semimeter.bean.GroupedResult;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.PathToken;
import org.semispace.semimeter.bean.TokenizedPathInfo;
import org.semispace.semimeter.bean.mongo.MeterHit;
import org.semispace.semimeter.bean.mongo.PathElements;
import org.semispace.semimeter.dao.AbstractSemiMeterDaoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Repository("semimeterDao")
public class SemiMeterDaoMongo extends AbstractSemiMeterDaoImpl {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDaoMongo.class);
    private DateFormat df = new SimpleDateFormat("yy-MM-dd-HH-mm");

    @Autowired
    MongoTemplate mongoTemplate;

    @PostConstruct
    public void onCreate() {
        mongoTemplate.getDefaultCollection().ensureIndex((DBObject) JSON.parse("{'day.count': -1}"));
        mongoTemplate.getDefaultCollection().ensureIndex((DBObject) JSON.parse("{'day.last180minutes': -1}"));
        mongoTemplate.getDefaultCollection().ensureIndex((DBObject) JSON.parse("{'day.last15minutes': -1}"));
        mongoTemplate.getDefaultCollection()
                .ensureIndex((DBObject) JSON.parse("{'id':1, 'sectionId':1, 'publicationId':1, 'type':1}"));
        mongoTemplate.getCollection("sums").ensureIndex((DBObject) JSON
                .parse("{'time.ts':1, 'time.year':1, 'time.month':1, 'time.day':1, 'time.hour':1, 'time.minute':1}"));
        mongoTemplate.getCollection("sums").ensureIndex((DBObject) JSON.parse("{'time.ts': 1}"));
    }


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
            sb.append("{ '$inc': ");
            sb.append(" { 'day.count' : " + item.getAccessNumber() + ", ");
            sb.append("   'day.last15minutes' : " + item.getAccessNumber() + ", ");
            sb.append("   'day.last180minutes' : " + item.getAccessNumber() + ", ");
            sb.append("   'day.hours." + hour + ".count' : " + item.getAccessNumber() + ",  ");
            sb.append("   'day.hours." + hour + ".minutes." + minute + ".count' : " + item.getAccessNumber() + "  ");
            sb.append("}}");

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
            final int maxResults, String sortBy) throws IllegalArgumentException {
        List<GroupedResult> result = new ArrayList<GroupedResult>();
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(startAt);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        long startMinute = cal.getTimeInMillis();
        cal.setTimeInMillis(endAt);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        long endMinute = cal.getTimeInMillis();

        final long endMinus15 = endMinute - 15 * 60 * 1000;
        final long endMinus180 = endMinute - 3 * 60 * 60 * 1000;

        BasicDBObject toFind = new BasicDBObject();
        for (PathToken token : query.getPathTokens()) {
            if ("sectionId".equals(token.getTokenAlias()) || "publicationId".equals(token.getTokenAlias())) {
                if (token.getValue() != null) {
                    toFind.append(token.getTokenAlias(), Integer.valueOf(token.getValue()));
                }
            } else if ("articleType".equals(token.getTokenAlias())) {
                if (token.getValue() != null) {
                    toFind.append("type", token.getValue());
                }
            }
        }

        BasicDBObject sortObj = new BasicDBObject();
        if (sortBy != null && "last15minutes".equals(sortBy)) {
            sortObj.append("day.last15minutes", -1);
        } else if (sortBy != null && "last180minutes".equals(sortBy)) {
            sortObj.append("day.last180minutes", -1);
        } else {
            sortObj.append("day.count", -1);
        }

        groupedSumsMemory(maxResults, result, endMinus15, endMinus180, toFind, sortObj);

        return result;
    }

    private void groupedSumsMemory(final int maxResults, final List<GroupedResult> result, final long endMinus15,
            final long endMinus180, final BasicDBObject toFind, final DBObject sortObj) {
        BasicDBObject keys = new BasicDBObject("id", 1);
        DBCursor dbResult = mongoTemplate.getDefaultCollection().find(toFind, keys).sort(sortObj).limit(maxResults);

        while (dbResult.hasNext()) {
            DBObject row = dbResult.next();
            Object docId = row.get("_id");
            int id = (Integer) row.get("id");

            DBObject doc = mongoTemplate.getDefaultCollection().findOne(new BasicDBObject("_id", docId));

            DBObject day = (DBObject) doc.get("day");
            DBObject hours = (DBObject) day.get("hours");

            GroupedResult gr = new GroupedResult();
            gr.setCount((Integer) day.get("count"));
            gr.setKey(String.valueOf(id));
            gr.setKeyName("articleId");
            gr.getSplitCounts().put("last180minutes", (Integer) day.get("last180minutes"));
            gr.getSplitCounts().put("last15minutes", (Integer) day.get("last15minutes"));

            Map<String, Integer> trend = gr.getTrend();

            for(String key : hours.keySet()) {
                DBObject hour = (DBObject) hours.get(key);
                trend.put(key, (Integer) hour.get("count"));
            }

            gr.setTrend(trend);
            result.add(gr);

        }
    }


    @Override
    public List<GroupedResult> getHourlySums(final Integer publicationId, final Integer sectionId) {
        Map<String, GroupedResult> result = new TreeMap<String, GroupedResult>();
        if (publicationId == null) {
            if (sectionId != null) {
                throw new IllegalArgumentException("cant have sectionId without publicationId as parameters.");
            }
            //total network
            DBObject sortObj = (DBObject) JSON.parse("{'time.ts': 1}");
            DBCursor dbResult = mongoTemplate.getCollection("sums").find().sort(sortObj);
            while (dbResult.hasNext()) {
                DBObject sum = dbResult.next();
                Long ts = (Long) ((DBObject) sum.get("time")).get("ts");
                String time = df.format(new Date(ts));

                GroupedResult groupedResult;
                if (result.containsKey(time)) {
                    groupedResult = result.get(time);
                } else {
                    groupedResult = new GroupedResult();
                }
                groupedResult.setKey(time);
                groupedResult.setCount(groupedResult.getCount() + (Integer) sum.get("total"));
                groupedResult.setKeyName("timestamp");
                addHourlySplit("article", sum, groupedResult);
                addHourlySplit("album", sum, groupedResult);
                addHourlySplit("video", sum, groupedResult);
                addHourlySplit("other", sum, groupedResult);
                result.put(time, groupedResult);
            }
        } else {
            DBObject query = null;
            if (sectionId == null) {
                query = new BasicDBObject("publicationId", publicationId);
            } else {
                query = new BasicDBObject("sectionId", sectionId);
            }

            hourlySumsMemory(result, query);

        }
        List<GroupedResult> list = new ArrayList<GroupedResult>(result.values().size());
        list.addAll(result.values());
        return list;
    }

    private void addHourlySplit(final String attribute, final DBObject sum, final GroupedResult groupedResult) {
        Integer val = groupedResult.getSplitCounts().get(attribute) == null ? 0 :
                groupedResult.getSplitCounts().get(attribute);
        Integer newVal = sum.get(attribute) == null ? 0 : (Integer) sum.get(attribute);

        groupedResult.getSplitCounts().put(attribute, val + newVal);
    }

    private void hourlySumsMemory(final Map<String, GroupedResult> result, final DBObject query) {
        DBCursor dbResult = mongoTemplate.getDefaultCollection().find(query);
        while (dbResult.hasNext()) {
            DBObject row = dbResult.next();
            DBObject day = (DBObject) row.get("day");
            DBObject hours = (DBObject) day.get("hours");
            String type = (String) row.get("type");
            for (String hourString : hours.keySet()) {
                DBObject hourObject = (DBObject) hours.get(hourString);
                DBObject minutes = (DBObject) hourObject.get("minutes");
                for (String minuteString : minutes.keySet()) {
                    DBObject minuteObject = (DBObject) minutes.get(minuteString);
                    Integer count = (Integer) minuteObject.get("count");
                    GroupedResult gr;
                    if (result.containsKey(minuteString)) {
                        gr = result.get(minuteString);
                    } else {
                        gr = new GroupedResult();
                        Long ts = Long.valueOf(minuteString);
                        String time = df.format(new Date(ts));
                        gr.setKey(time);
                        gr.setKeyName("minute");
                        gr.setCount(0);
                        result.put(minuteString, gr);
                    }
                    gr.setCount(gr.getCount() + count);
                    if (gr.getSplitCounts().containsKey(type)) {
                        gr.getSplitCounts().put(type, gr.getSplitCounts().get(type) + count);
                    } else {
                        gr.getSplitCounts().put(type, count);
                    }
                }
            }
        }
    }

    @Override
    public void deleteEntriesOlderThanMillis(final long millis) {
        long now = System.currentTimeMillis();
        long when = now - millis;
        long before180min = now - 180 * 60 * 1000;
        long before15min = now - 15 * 60 * 1000;
        deleteOldSums(when);
        deleteOldMinutes(when, before180min, before15min);
    }

    private void deleteOldMinutes(long before24h, long before180min, long before15min) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(before24h);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        long targetHour = cal.getTimeInMillis();

        DBCursor result = mongoTemplate.getDefaultCollection().find(new BasicDBObject(), new BasicDBObject("_id", 1));
        while (result.hasNext()) {
            DBObject doc = result.next();

            //start a new "session" for each document. not sure if this actually helps anything consistency-wise
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
                log.trace("no hours in document, remove it: {}", doc);
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
                            if (minutemillis < before24h) {
                                if (log.isTraceEnabled()) {
                                    log.trace("removing minute " + minutemillis + " because it is older than " +
                                            before24h);
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

            docChanged |= updateTrendCounters(doc, before180min, before15min);

            if (docChanged) {
                mongoTemplate.getDefaultCollection().save(doc);
            }
            mongoTemplate.getDefaultCollection().getDB().requestDone();
        }
    }

    private boolean updateTrendCounters(final DBObject doc, final long before180min, final long before15min) {
        long oneHour = 60 * 60 * 1000;
        BasicDBObject day = (BasicDBObject) doc.get("day");
        DBObject hours = (DBObject) day.get("hours");

        int last15Counter = 0;
        int last180Counter = 0;
        for (String h : hours.keySet()) {
            long hourmillis = Long.valueOf(h);
            if (hourmillis >= (before180min - oneHour)) {
                DBObject currentHour = (DBObject) hours.get(h);
                DBObject minutes = (DBObject) currentHour.get("minutes");

                for (String m : minutes.keySet()) {
                    long minutemillis = Long.valueOf(m);
                    DBObject obj = null;
                    if (minutemillis >= before180min) {
                        obj = (DBObject) minutes.get(m);
                        last180Counter += (Integer) obj.get("count");
                    }
                    if (minutemillis >= before15min) {
                        last15Counter += (Integer) obj.get("count");
                    }

                }
            }
        }

        int oldVal = day.get("last15minutes") == null ? 0 : (Integer) day.get("last15minutes");
        boolean objChanged = false;
        if (oldVal != last15Counter) {
            objChanged = true;
            day.put("last15minutes", last15Counter);
        }

        oldVal = day.get("last180minutes") == null ? 0 : (Integer) day.get("last180minutes");
        if (oldVal != last180Counter) {
            objChanged = true;
            day.put("last180minutes", last180Counter);
        }

        return objChanged;
    }


    private void deleteOldSums(long when) {
        DBCursor result =
                mongoTemplate.getCollection("sums").find((DBObject) JSON.parse("{'time.ts': {'$lt': " + when + "}}"));

        while (result.hasNext()) {
            mongoTemplate.getCollection("sums").remove(result.next());
        }
    }
}
