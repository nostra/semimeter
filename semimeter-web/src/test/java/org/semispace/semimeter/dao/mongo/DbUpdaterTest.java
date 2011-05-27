package org.semispace.semimeter.dao.mongo;

import com.mongodb.BasicDBObject;
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


                    upsert(mongoTemplate, when, count, path);


                }

                line = reader.readLine();
            }
        } else {
            fail("file not found: " + file);
        }
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

        BasicDBObject query = new BasicDBObject();
        PathElements pathElements = MeterHit.calcPath(path, "/");
        query.append("id", Integer.valueOf(pathElements.getE4()).intValue());
        query.append("sectionId", Integer.valueOf(pathElements.getE3()).intValue());
        query.append("publicationId", Integer.valueOf(pathElements.getE2()).intValue());
        query.append("type", pathElements.getE1());

        StringBuilder sb = new StringBuilder();
        sb.append(" { '$inc': ");
        sb.append("      { 'day.count' : " + count + ", ");
        sb.append("        'day.hours." + hour + ".count' : " + count + ",  ");
        sb.append("        'day.hours." + hour + ".minutes." + minute + ".count' : " + count + ",  ");
        sb.append(
                "        'day.hours." + hour + ".minutes." + minute + ".seconds." + second + ".count' : " +
                        count + "  ");
        sb.append(" } }");

        DBObject update = (DBObject) JSON.parse(sb.toString());

        mongoTemplate.getDefaultCollection().update(query, update, true, false, WriteConcern.NONE);
    }
}
