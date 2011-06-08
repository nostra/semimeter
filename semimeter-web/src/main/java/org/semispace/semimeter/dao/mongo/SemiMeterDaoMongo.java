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
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
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
        mongoTemplate.getDefaultCollection().ensureIndex((DBObject) JSON.parse("{'day.count': 1}"));
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
            final int maxResults) throws IllegalArgumentException {
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

        /**
         * The mapReduce solution is a lot slower, but if optimized in a way that it is only executed every minute, and in
         * between the "out" collection is queried, then it can be a good solution, too.
         */
        groupedSumsMemory(maxResults, result, endMinus15, endMinus180, toFind);

        //groupedSumsMapReduce(toFind, query, maxResults, result, startMinute, endMinute, endMinus15, endMinus180);

        return result;
    }

    private void groupedSumsMemory(final int maxResults, final List<GroupedResult> result, final long endMinus15,
            final long endMinus180, final BasicDBObject toFind) {
        BasicDBObject keys = new BasicDBObject("id", 1);
        keys.append("day.count", 1);
        DBCursor dbResult =
                mongoTemplate.getDefaultCollection().find(toFind, keys).sort(new BasicDBObject("day.count", -1))
                        .limit(maxResults);

        while (dbResult.hasNext()) {
            DBObject row = dbResult.next();
            Object docId = row.get("_id");
            int id = (Integer) row.get("id");
            int count = (Integer) ((DBObject) row.get("day")).get("count");
            DBObject doc = mongoTemplate.getDefaultCollection().findOne(new BasicDBObject("_id", docId));

            DBObject day = (DBObject) doc.get("day");
            DBObject hours = (DBObject) day.get("hours");
            String type = (String) doc.get("type");

            GroupedResult gr = new GroupedResult();
            gr.setCount(count);
            gr.setKey(String.valueOf(id));
            gr.setKeyName("articleId");

            for (String hourString : hours.keySet()) {
                DBObject hourObject = (DBObject) hours.get(hourString);
                DBObject minutes = (DBObject) hourObject.get("minutes");
                for (String minuteString : minutes.keySet()) {
                    Long ts = Long.valueOf(minuteString);

                    if (ts >= endMinus180) {
                        DBObject minuteObject = (DBObject) minutes.get(minuteString);
                        Integer minuteCount = (Integer) minuteObject.get("count");

                        if (gr.getSplitCounts().containsKey("last180minutes")) {
                            gr.getSplitCounts()
                                    .put("last180minutes", gr.getSplitCounts().get("last180minutes") + minuteCount);
                        } else {
                            gr.getSplitCounts().put("last180minutes", minuteCount);
                        }

                        if (ts >= endMinus15) {
                            if (gr.getSplitCounts().containsKey("last15minutes")) {
                                gr.getSplitCounts()
                                        .put("last15minutes", gr.getSplitCounts().get("last15minutes") + minuteCount);
                            } else {
                                gr.getSplitCounts().put("last15minutes", minuteCount);
                            }
                        }

                    }
                }
            }
            result.add(gr);

        }
    }

    private void groupedSumsMapReduce(final DBObject toFind, final TokenizedPathInfo query, final int maxResults,
            final List<GroupedResult> result, final long startMinute, final long endMinute, final long endMinus15,
            final long endMinus180) {
        StringBuffer sb = new StringBuffer();
        sb.append("function() {");
        sb.append("  for (h in this.day.hours) {");
        sb.append("     var hour = this.day.hours[h];");
        sb.append("     for (m in hour.minutes) { ");
        //sb.append("        if (m > (new Date().getTime() - 1000*60*60*24)) { ");
        sb.append("        if (m > " + startMinute + " && m <= " + endMinute + ") { ");
        sb.append("            var current = 0;");
        sb.append("            var latest = 0;");
        sb.append("            var cnt = hour.minutes[m].count;");
        //sb.append("            if (m > (new Date().getTime() - 1000*60*15)) {");
        sb.append("            if (m > " + endMinus15 + ") {");
        sb.append("                current += cnt;");
        sb.append("            }");
        //sb.append("            if (m > (new Date().getTime() - 1000*60*60*3)) {");
        sb.append("            if (m > " + endMinus180 + ") {");
        sb.append("                latest += cnt;");
        sb.append("            }");
        sb.append("           emit (''+this.id, {'total':cnt, 'current': current, 'latest': latest} ); ");
        sb.append("        } ");
        sb.append("      } ");
        sb.append("  }  ");
        sb.append("};");
        String map = sb.toString();
        /*String s = map;
        while (s.contains("  ")) {
            s = s.replace("  ", " ");
        }
        System.out.println("var map = " + s);*/

        sb.setLength(0);
        sb.append("function(key, values) { ");
        sb.append("    var result = {'total':0, 'current':0, 'latest':0}; ");
        sb.append("    for (v in values) {");
        sb.append("        var obj = values[v];");
        sb.append("        result = {");
        sb.append("            'total':result.total + obj.total, ");
        sb.append("            'current':result.current + obj.current, ");
        sb.append("            'latest':result.latest + obj.latest");
        sb.append("        };");
        sb.append("    }; ");
        sb.append("    return result; ");
        sb.append("};");
        String reduce = sb.toString();
        /*s = reduce;
        while (s.contains("  ")) {
            s = s.replace("  ", " ");
        }
        System.out.println("var reduce = " + s.replace("  ", " "));*/

        String outputCollectionName = "groupedSum_" + query.buildPathFromTokens() + "_" + maxResults;

        MapReduceCommand command =
                new MapReduceCommand(mongoTemplate.getDefaultCollection(), map, reduce, outputCollectionName,
                        MapReduceCommand.OutputType.REPLACE, toFind);

        mongoTemplate.getDefaultCollection().mapReduce(command);

        DBObject sortObj = BasicDBObjectBuilder.start("value.total", -1).get();
        DBCursor dbResult = mongoTemplate.getCollection(outputCollectionName).find().sort(sortObj).limit(maxResults);

        while (dbResult.hasNext()) {
            DBObject row = dbResult.next();
            DBObject value = (DBObject) row.get("value");
            GroupedResult gr = new GroupedResult();
            gr.setCount(((Double) value.get("total")).intValue());
            gr.setKey(String.valueOf(row.get("_id")));
            gr.setKeyName("articleId");
            gr.getSplitCounts().put("last15minutes", ((Double) value.get("current")).intValue());
            gr.getSplitCounts().put("last180minutes", ((Double) value.get("latest")).intValue());
            result.add(gr);
        }
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
            DBObject query = null;
            if (sectionId == null) {
                query = new BasicDBObject("publicationId", publicationId);
            } else {
                query = new BasicDBObject("sectionId", sectionId);
            }

            /**
             * The mapReduce solution is a lot slower, but if optimized in a way that it is only executed every minute, and in
             * between the "out" collection is queried, then it can be a good solution too.
             */

            //hourlySumsMapReduce(publicationId, sectionId, result, query);

            /**
             * the memory solution just fetches all relevant documents and calculates the rest in memory. if this get's too tough
             * for the hardware, the mapReduce approach should be looked at again.
             *
             */
            hourlySumsMemory(result, query);

        }
        return result;
    }

    private void hourlySumsMemory(final List<GroupedResult> result, final DBObject query) {
        DBCursor dbResult = mongoTemplate.getDefaultCollection().find(query);
        Map<String, GroupedResult> map = new TreeMap<String, GroupedResult>();

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
                    if (map.containsKey(minuteString)) {
                        gr = map.get(minuteString);
                    } else {
                        gr = new GroupedResult();
                        Long ts = Long.valueOf(minuteString);
                        String time = df.format(new Date(ts));
                        gr.setKey(time);
                        gr.setKeyName("minute");
                        gr.setCount(0);
                        map.put(minuteString, gr);
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
        result.addAll(map.values());
    }

    private void hourlySumsMapReduce(final Integer publicationId, final Integer sectionId,
            final List<GroupedResult> result, final DBObject query) {
        StringBuilder sb = new StringBuilder();
        //function() { for (h in this.day.hours) { var hour = this.day.hours[h]; for (m in hour.minutes) { var cnt = hour.minutes[m].count; var counter = { 'total': cnt, 'article': this.type == 'article' ? cnt : 0, 'album': this.type == 'album' ? cnt : 0, 'video': this.type == 'video' ? cnt : 0 }; emit(m, counter); } }};
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
        //String s = map;
        //while (s.contains("  ")) {
        //    s = s.replace("  ", " ");
        //}
        //System.out.println("var map = " + s);

        sb.setLength(0);
        //function(key, values) { var result = { total: 0, article: 0, album:0, video:0 }; for (v in values) { var cnt = values[v]; result.total += cnt.total; result.article += cnt.article; result.album += cnt.album; result.video += cnt.video }; return result; };
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
        //s = reduce;
        //while (s.contains("  ")) {
        //    s = s.replace("  ", " ");
        //}
        //System.out.println("var reduce = " + s.replace("  ", " "));
        String outputCollectionName = "sums_" + publicationId + "_" + sectionId;

        MapReduceCommand command =
                new MapReduceCommand(mongoTemplate.getDefaultCollection(), map, reduce, outputCollectionName,
                        MapReduceCommand.OutputType.REPLACE, query);

        mongoTemplate.getDefaultCollection().mapReduce(command);

        DBCursor dbResult = mongoTemplate.getCollection(outputCollectionName).find().sort(new BasicDBObject("_id", 1));
        while (dbResult.hasNext()) {
            DBObject sum = dbResult.next();
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
