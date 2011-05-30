package org.semispace.semimeter.dao.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import com.mongodb.util.JSON;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.semispace.semimeter.bean.mongo.MeterHit;
import org.semispace.semimeter.bean.mongo.PathElements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/context/mongo-test.context.xml"})
public class DbUpdaterTest {

    @Autowired
    private Mongo mongo;

    @Test
    @Ignore("only use to import test data (from mysql dump file")
    public void doIt() throws IOException {
        MongoTemplate mongoTemplate = new MongoTemplate(mongo, "mittari", "meter");
        File file = new File("/home/sven/dev/trunk/api/maven-projects/v3/mittari/meter.sql");
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                Pattern patter = Pattern.compile("\\((.*?)\\)");
                Matcher matcher = patter.matcher(line);
                while (matcher.find()) {
                    String hit = matcher.group(1);
                    String[] tokens = hit.split(",");
                    long when = Long.valueOf(tokens[0]).longValue();
                    int count = Integer.valueOf(tokens[1]);
                    String path = tokens[2].replace("'", "");
                    MeterHit meterHit = new MeterHit(when, path, "/", count);

                    mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), meterHit);
                }

                line = reader.readLine();
            }
        } else {
            fail("file not found: " + file);
        }
    }

    @Test
    @Ignore("only use to import test data (from mysql dump file")
    public void doIt2() throws IOException {
        MongoTemplate mongoTemplate = new MongoTemplate(mongo, "mittari", "meter");
        File file = new File("/home/sven/dev/trunk/api/maven-projects/v3/mittari/meter.sql");
        if (file.exists()) {
            int cnt = 0;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();

            Map<DBObject, DBObject> map = new HashMap<DBObject, DBObject>();
            Map<DBObject, DBObject> sumsmap = new HashMap<DBObject, DBObject>();
            while (line != null) {
                Pattern patter = Pattern.compile("\\((.*?)\\)");
                Matcher matcher = patter.matcher(line);

                while (matcher.find()) {
                    String hit = matcher.group(1);
                    String[] tokens = hit.split(",");
                    long when = Long.valueOf(tokens[0]).longValue();
                    int count = Integer.valueOf(tokens[1]);
                    String path = tokens[2].replace("'", "");

                    long l = System.currentTimeMillis();
                    //upsert(mongoTemplate, when, count, path);
                    updateList(when, count, path, map, sumsmap);
                    long diff = (System.currentTimeMillis() - l);
                    if (diff > 1) {
                        System.out.println("upsert done in " + diff + "ms");
                    }
                    cnt++;

                    if (cnt % 1000 == 0) {
                        System.out.println(cnt);

                    }

                    if (cnt % 50000 == 0) {
                        mongo.getConnector().requestDone();

                    }
                }

                line = reader.readLine();
            }
            for (DBObject o : map.values()) {
                mongoTemplate.getDefaultCollection().insert(o);
            }
            for (DBObject o : sumsmap.values()) {
                mongoTemplate.getCollection("sums").insert(o);
            }

        } else {
            fail("file not found: " + file);
        }
    }

    private void updateList(final long when, final int count, final String path,
            final Map<DBObject, DBObject> articleMap, final Map<DBObject, DBObject> sumsMap) {
        //some time calculations
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(when);
        cal.set(Calendar.MILLISECOND, 0);
        long second = cal.getTimeInMillis();
        cal.set(Calendar.SECOND, 0);
        long minute = cal.getTimeInMillis();
        cal.set(Calendar.MINUTE, 0);
        long hour = cal.getTimeInMillis();
        PathElements pathElements = MeterHit.calcPath(path, "/");
        cal.setTimeInMillis(when);

        BasicDBObject query = new BasicDBObject();
        query.append("id", Integer.valueOf(pathElements.getE4()).intValue());
        query.append("sectionId", Integer.valueOf(pathElements.getE3()).intValue());
        query.append("publicationId", Integer.valueOf(pathElements.getE2()).intValue());
        query.append("type", pathElements.getE1());

        BasicDBObject art = (BasicDBObject) articleMap.get(query);
        if (art == null) {
            art = new BasicDBObject();
            art.append("id", Integer.valueOf(pathElements.getE4()).intValue());
            art.append("sectionId", Integer.valueOf(pathElements.getE3()).intValue());
            art.append("publicationId", Integer.valueOf(pathElements.getE2()).intValue());
            art.append("type", pathElements.getE1());
            articleMap.put(query, art);
        }

        BasicDBObject day = (BasicDBObject) art.get("day");
        if (day == null) {
            day = new BasicDBObject();
            art.append("day", day);
        }
        Integer daycount = (Integer) day.get("count");
        if (daycount == null) {
            daycount = 0;
            day.append("count", daycount);
        }
        day.put("count", daycount + count);

        BasicDBObject hours = (BasicDBObject) day.get("hours");
        if (hours == null) {
            hours = new BasicDBObject();
            day.append("hours", hours);
        }

        BasicDBObject currentHour = (BasicDBObject) hours.get("" + hour);
        if (currentHour == null) {
            currentHour = new BasicDBObject();
            hours.append("" + hour, currentHour);
        }
        Integer hourcount = (Integer) currentHour.get("count");
        if (hourcount == null) {
            hourcount = 0;
        }
        currentHour.put("count", hourcount + count);

        BasicDBObject minutes = (BasicDBObject) currentHour.get("minutes");
        if (minutes == null) {
            minutes = new BasicDBObject();
            currentHour.append("minutes", minutes);
        }

        BasicDBObject currentMinute = (BasicDBObject) minutes.get("" + minute);
        if (currentMinute == null) {
            currentMinute = new BasicDBObject();
            minutes.append("" + minute, currentMinute);
        }
        Integer minuteCount = (Integer) currentMinute.get("count");
        if (minuteCount == null) {
            minuteCount = 0;
        }
        currentMinute.put("count", minuteCount + count);

        //System.out.println(art);

        query = new BasicDBObject();
        BasicDBObject time = new BasicDBObject();
        query.append("time", time);
        time.append("ts", minute);
        time.append("year", cal.get(Calendar.YEAR));
        time.append("month", cal.get(Calendar.MONTH));
        time.append("day", cal.get(Calendar.DAY_OF_MONTH));
        time.append("hour", cal.get(Calendar.HOUR_OF_DAY));
        time.append("minute", cal.get(Calendar.MINUTE));

        BasicDBObject sum = (BasicDBObject) sumsMap.get(query);
        if (sum == null) {
            sum = (BasicDBObject) query.clone();
            sumsMap.put(query, sum);
        }

        sum.put("total", (sum.get("total") != null) ? ((Integer) sum.get("total") + count) : (count));
        String TYPE = "article";
        if (pathElements.getE1().equals(TYPE)) {
            sum.put(TYPE, (sum.get(TYPE) != null) ? ((Integer) sum.get(TYPE) + count) : (count));
        } else if (pathElements.getE1().equals("album")) {
            TYPE = "album";
            sum.put(TYPE, (sum.get(TYPE) != null) ? ((Integer) sum.get(TYPE) + count) : (count));
        } else if (pathElements.getE1().equals("video")) {
            TYPE = "video";
            sum.put(TYPE, (sum.get(TYPE) != null) ? ((Integer) sum.get(TYPE) + count) : (count));
        } else {
            TYPE = "other";
            sum.put(TYPE, (sum.get(TYPE) != null) ? ((Integer) sum.get(TYPE) + count) : (count));

        }
    }

    private Object getObj(final BasicDBObject art, final String key, String objType) {
        Object test = art.get(key);
        if (test == null) {
            if (objType.endsWith("dbobject")) {
                art.append(key, new BasicDBObject());
            } else if (objType.equals("number")) {
                art.append(key, 0);
            }
        }
        return art.get(key);
    }

    private void upsert(final MongoTemplate mongoTemplate, final long when, final int count, final String path) {
        //some time calculations
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(when);
        cal.set(Calendar.MILLISECOND, 0);
        long second = cal.getTimeInMillis();
        cal.set(Calendar.SECOND, 0);
        long minute = cal.getTimeInMillis();
        cal.set(Calendar.MINUTE, 0);
        long hour = cal.getTimeInMillis();
        PathElements pathElements = MeterHit.calcPath(path, "/");

        BasicDBObject query = new BasicDBObject();
        query.append("id", Integer.valueOf(pathElements.getE4()).intValue());
        query.append("sectionId", Integer.valueOf(pathElements.getE3()).intValue());
        query.append("publicationId", Integer.valueOf(pathElements.getE2()).intValue());
        query.append("type", pathElements.getE1());
        StringBuilder sb = new StringBuilder();
        sb.append(" { '$inc': ");
        sb.append("      { 'counts.count' : " + count + ", ");
        //sb.append("        'counts.hours." + hour + ".count' : " + count + ",  ");
        //sb.append("        'counts.hours." + hour + ".minutes." + minute + ".count' : " + count + "  ");
        //sb.append(" ,       'day.hours." + hour + ".minutes." + minute + ".seconds." + second + ".count' : " + count +"  ");
        sb.append(" } }");

        DBObject update = (DBObject) JSON.parse(sb.toString());
        mongoTemplate.getDefaultCollection().update(query, update, true, false, WriteConcern.NONE);

    }

    private void upsert2(final MongoTemplate mongoTemplate, final long when, final int count, final String path) {
        //get calendar
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(when);

        //create time document template
        DBObject timeObj =
                BasicDBObjectBuilder.start("year", cal.get(Calendar.YEAR)).add("month", cal.get(Calendar.MONTH))
                        .add("day", cal.get(Calendar.DAY_OF_MONTH)).add("hour", cal.get(Calendar.HOUR_OF_DAY))
                        .add("minute", cal.get(Calendar.MINUTE)).add("second", cal.get(Calendar.SECOND)).get();

        //create article document template
        BasicDBObject query = new BasicDBObject();
        PathElements pathElements = MeterHit.calcPath(path, "/");
        query.append("id", Integer.valueOf(pathElements.getE4()).intValue());
        query.append("sectionId", Integer.valueOf(pathElements.getE3()).intValue());
        query.append("publicationId", Integer.valueOf(pathElements.getE2()).intValue());
        query.append("type", pathElements.getE1());
        query.append("time", timeObj);

        //upsert, increment count
        DBObject update = (DBObject) JSON.parse(" { '$inc': { 'count' : " + count + "} }");
        //mongoTemplate.getDefaultCollection().update(query, update, true, false, WriteConcern.NONE);

        query.append("count", count);
        mongoTemplate.getDefaultCollection().insert(query);

        //create sums document template
        query = new BasicDBObject();
        query.append("time", timeObj);

        //upsert, increment multiple counters
        StringBuilder sb = new StringBuilder();
        sb.append(" { '$inc': ");
        sb.append("      { 'total' : ").append(count);
        if ("article".equals(pathElements.getE1())) {
            sb.append(", 'article' : ").append(count);
        } else if ("album".equals(pathElements.getE1())) {
            sb.append(", 'album' : ").append(count);
        } else if ("video".equals(pathElements.getE1())) {
            sb.append(", 'video' : ").append(count);
        } else {
            sb.append(", 'other' : ").append(count);
        }
        sb.append(" } }");

        //  mongoTemplate.getCollection("sums").update(query, update, true, false);

    }
}
