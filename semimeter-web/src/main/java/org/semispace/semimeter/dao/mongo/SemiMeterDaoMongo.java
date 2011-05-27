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
import com.mongodb.CommandResult;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.GroupCommand;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
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
import org.springframework.data.document.mongodb.query.Criteria;
import org.springframework.data.document.mongodb.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository("semimeterDao")
public class SemiMeterDaoMongo extends AbstractSemiMeterDaoImpl {
    private static final Logger log = LoggerFactory.getLogger(SemiMeterDaoMongo.class);

    @Autowired
    MongoTemplate mongoTemplate;

    private ReadWriteLock rwl = new ReentrantReadWriteLock();

    @Override
    public int size() {
        int result = -1;
        rwl.readLock().lock();
        try {
            result = (int) mongoTemplate.getDefaultCollection().count();
        } finally {
            rwl.readLock().unlock();
        }

        return result;
    }

    @Override
    public boolean isAlive() {
        rwl.readLock().lock();
        try {
            Long result = mongoTemplate.getDefaultCollection().count();
            if (result != null) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Table probably not yet created. Got (intentionally masked) " + e.getMessage());
        } finally {
            rwl.readLock().unlock();
        }
        return false;
    }

    @Override
    public Long sumItems(long startAt, long endAt, String path) {
        Long result = Long.valueOf(-1);
        rwl.readLock().lock();
        try {
            PathElements pathElements = MeterHit.calcPath(path, "/");

            //building query condition, starting with time window
            BasicDBObjectBuilder builder = BasicDBObjectBuilder
                    .start("when", BasicDBObjectBuilder.start("$gt", startAt).add("$lte", endAt).get());

            //for each non-null member of pathElements, add one DBObject to builder
            if (pathElements != null) {
                for (int i = 1; i <= 5; i++) {
                    try {
                        Object test = pathElements.getClass().getMethod("getE" + i).invoke(pathElements);
                        if (test != null) {
                            builder.add("pathElements.e" + i, test);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            final DBObject cond = builder.get();

            //initialize aggregation variable
            final DBObject initial = new BasicDBObject("groupedSum", 0);
            //define reduce function. 
            final String reduce = "function (doc, prev) {prev.groupedSum += doc.count; }";

            //compose final groupCommand and execute
            final GroupCommand groupCommand =
                    new GroupCommand(mongoTemplate.getDefaultCollection(), null, cond, initial, reduce, null);

            //System.out.println(groupCommand.toDBObject());

            DBObject groupResult = mongoTemplate.getDefaultCollection().group(groupCommand);

            //System.out.println(groupResult);

            long sum = 0;
            for (String key : groupResult.keySet()) {
                CommandResult groupedItem = (CommandResult) groupResult.get(key);
                sum += groupedItem.getInt("groupedSum");
            }
            result = sum;
        } finally {
            rwl.readLock().unlock();
        }

        return result;
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
        List<GroupedResult> result = new ArrayList<GroupedResult>();
        /*
           SELECT SUBSTRING_INDEX(path, '/', -1) as article_id,
                  sum(counted) as cnt
           FROM meter m
           where path like '/album/%/%/%'
           group by article_id
           order by cnt desc
           limit 10
        */

        //building query condition, starting with time window
        BasicDBObjectBuilder builder =
                BasicDBObjectBuilder.start("when", BasicDBObjectBuilder.start("$gt", startAt).add("$lte", endAt).get());
        PathElements pathElements = MeterHit.calcPath(query.buildPathFromTokens(), query.getPathTokenDelimeter());
        //for each non-null member of pathElements, add one DBObject to builder
        if (pathElements != null) {
            for (int i = 1; i <= 5; i++) {
                try {
                    Object test = pathElements.getClass().getMethod("getE" + i).invoke(pathElements);
                    if (test != null) {
                        builder.add("pathElements.e" + i, test);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        final DBObject cond = builder.get();

        long t = System.currentTimeMillis();

        /* group function has 10.000 (20.000 in v1.8) keys limit (not enough). it's also slower than java processing. */
        //useMongoGroup(maxResults, result, cond);

        /* map reduce takes like 45-50 seconds */
        useMapReduce(maxResults, result, cond);

        /* this variant writes to a result collection. */
        //useMapReduce2(maxResults, result, cond);

        /* proecssing in java takes around 5 seconds, but is likely to suffer from higher load */
        //useFindOnly(maxResults, result, cond);

        System.out.println("trace: " + (System.currentTimeMillis() - t) + "ms");

        return result;
    }

    private void useMapReduce(final int maxResults, final List<GroupedResult> result, final DBObject cond) {
        final String mapfunction = "function(){ emit(this.pathElements.e4, this.count); }";
        final String reduce = "function(key, values) { var total = 0; for (v in values) { " +
                "total = total+values[v]; } return total; }";
        final String outputCollection = null;
        final OutputType type = OutputType.INLINE;
        final MapReduceCommand command =
                new MapReduceCommand(mongoTemplate.getDefaultCollection(), mapfunction, reduce, outputCollection, type,
                        cond);

        //        System.out.println("\ncommand: " + command.toDBObject());

        MapReduceOutput mrResult = mongoTemplate.getDefaultCollection().mapReduce(command);

        Iterator it = mrResult.results().iterator();
        while (it.hasNext()) {
            BasicDBObject obj = (BasicDBObject) it.next();

            final GroupedResult groupedResult = new GroupedResult();
            groupedResult.setKey(obj.getString("_id"));
            groupedResult.setKeyName("articleId");
            groupedResult.setCount(obj.getInt("value"));
            for (int i = 0; i < maxResults; i++) {
                //System.out.println("i: "+i);
                if (i < result.size()) {
                    // System.out.println("i < result.size: true");
                    if (groupedResult.getCount() > result.get(i).getCount()) {
                        //   System.out.println(groupedResult.getCount() + " > "+result.get(i).getCount()+": true");
                        result.add(i, groupedResult);
                        // System.out.println("added "+groupedResult+" at pos "+i);
                        break;
                    } else {
                        // System.out.println("count not bigger, skip");
                    }
                } else {
                    // System.out.println("out of list scope, just adding "+groupedResult+" at pos "+i);
                    result.add(i, groupedResult);
                    break;
                }
            }
            // System.out.println("result before trim: " + result);
            while (result.size() > maxResults) {
                result.remove(maxResults);
            }
        }

    }

    private void useMapReduce2(final int maxResults, final List<GroupedResult> result, final DBObject cond) {
        final String mapfunction = "function(){ emit(this.pathElements.e4, this.count); }";
        final String reduce = "function(key, values) { var total = 0; for (v in values) { " +
                "total = total+values[v]; } return total; }";
        final String outputCollection = "tmp_" + System.currentTimeMillis() + "_" + Math.random();
        final OutputType type = OutputType.REPLACE;
        final MapReduceCommand command =
                new MapReduceCommand(mongoTemplate.getDefaultCollection(), mapfunction, reduce, outputCollection, type,
                        cond);

        //        System.out.println("\ncommand: " + command.toDBObject());

        MapReduceOutput mrResult = mongoTemplate.getDefaultCollection().mapReduce(command);
        DBCursor dbResult =
                mrResult.getOutputCollection().find().sort(new BasicDBObject("value", -1)).limit(maxResults);

        //group by e4 and sum counts
        while (dbResult.hasNext()) {
            BasicDBObject row = (BasicDBObject) dbResult.next();
            System.out.println(row);
            final GroupedResult groupedResult = new GroupedResult();
            groupedResult.setKey(row.getString("_id"));
            groupedResult.setKeyName("articleId");
            groupedResult.setCount(row.getInt("value"));
            result.add(groupedResult);
        }

        mrResult.getOutputCollection().drop();
    }

    private void useFindOnly(final int maxResults, final List<GroupedResult> result, final DBObject cond) {
        DBCursor dbResult = mongoTemplate.getDefaultCollection().find(cond);
        Map<String, Integer> map = new HashMap<String, Integer>();

        //group by e4 and sum counts
        while (dbResult.hasNext()) {
            DBObject row = dbResult.next();
            //System.out.println(row);
            String key = (String) ((DBObject) row.get("pathElements")).get("e4");
            Integer count = (Integer) row.get("count");
            if (map.containsKey(key)) {
                map.put(key, map.get(key) + count);
            } else {
                map.put(key, count);
            }
        }

        //sort by counts desc and limit
        for (String key : map.keySet()) {
            final GroupedResult groupedResult = new GroupedResult();
            groupedResult.setKey(key);
            groupedResult.setKeyName("articleId");
            groupedResult.setCount(map.get(key));
            for (int i = 0; i < maxResults; i++) {
                //System.out.println("i: "+i);
                if (i < result.size()) {
                    // System.out.println("i < result.size: true");
                    if (groupedResult.getCount() > result.get(i).getCount()) {
                        //   System.out.println(groupedResult.getCount() + " > "+result.get(i).getCount()+": true");
                        result.add(i, groupedResult);
                        // System.out.println("added "+groupedResult+" at pos "+i);
                        break;
                    } else {
                        // System.out.println("count not bigger, skip");
                    }
                } else {
                    // System.out.println("out of list scope, just adding "+groupedResult+" at pos "+i);
                    result.add(i, groupedResult);
                    break;
                }
            }
            // System.out.println("result before trim: " + result);
            while (result.size() > maxResults) {
                result.remove(maxResults);
            }
        }
    }

    /*
     crashes when more than 10.000 keys are returned.
     */
    private void useMongoGroup(final int maxResults, final List<GroupedResult> result, final DBObject cond) {
        //initialize aggregation variable
        final DBObject initial = new BasicDBObject("groupedSum", 0);
        //define reduce function.
        final String reduce = "function (doc, prev) {prev.groupedSum += doc.count; }";

        //compose final groupCommand and execute
        //TODO: make key dynamics (check for groupedBy flag in query)
        final DBObject keys = new BasicDBObject("pathElements.e4", true);
        final GroupCommand groupCommand =
                new GroupCommand(mongoTemplate.getDefaultCollection(), keys, cond, initial, reduce, null);

        //System.out.println(groupCommand.toDBObject());

        DBObject dbResult = mongoTemplate.getDefaultCollection().group(groupCommand);

        //System.out.println("\nresult:" + dbResult);

        for (String key : dbResult.keySet()) {
            CommandResult commandResult = (CommandResult) dbResult.get(key);
            //System.out.println("commandResult: " + commandResult);

            final GroupedResult groupedResult = new GroupedResult();
            groupedResult.setKey(commandResult.getString("pathElements.e4"));
            groupedResult.setKeyName("articleId");
            groupedResult.setCount(commandResult.getInt("groupedSum"));
            for (int i = 0; i < maxResults; i++) {
                //System.out.println("i: "+i);
                if (i < result.size()) {
                    // System.out.println("i < result.size: true");
                    if (groupedResult.getCount() > result.get(i).getCount()) {
                        //   System.out.println(groupedResult.getCount() + " > "+result.get(i).getCount()+": true");
                        result.add(i, groupedResult);
                        // System.out.println("added "+groupedResult+" at pos "+i);
                        break;
                    } else {
                        // System.out.println("count not bigger, skip");
                    }
                } else {
                    // System.out.println("out of list scope, just adding "+groupedResult+" at pos "+i);
                    result.add(i, groupedResult);
                    break;
                }
            }
            // System.out.println("result before trim: " + result);
            while (result.size() > maxResults) {
                result.remove(maxResults);
            }
            //   System.out.println("result after trim: " + result);
        }
    }

    @Override
    public List<GroupedResult> getHourlySums(final Integer publicationId, final Integer sectionId) {
        return null;
    }

    @Override
    public void deleteEntriesOlderThanMillis(final long millis) {
        final Query q = Query.query(Criteria.where("when").lte(System.currentTimeMillis() - millis));
        mongoTemplate.findAndRemove(mongoTemplate.getDefaultCollectionName(), q, MeterHit.class);
    }

    @Override
    public void performInsertion(Collection<Item> items) {
        List<MeterHit> list = new ArrayList<MeterHit>();
        for (Item item : items) {
            list.add(new MeterHit(item.getWhen(), item.getPath(), "/", item.getAccessNumber()));
        }
        //if multiple semimeter instances try inserting documents for same when+path, thats just fine. hence no fancy concurrency protection here.
        mongoTemplate.insertList(mongoTemplate.getDefaultCollectionName(), list);
    }

}
