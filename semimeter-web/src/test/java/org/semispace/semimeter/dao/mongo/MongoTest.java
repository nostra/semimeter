package org.semispace.semimeter.dao.mongo;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MongoTest {
    private DB db;

    @Before
    public void setUp() throws Exception {
        Mongo m = new Mongo("127.0.0.1", 27017);
        db = m.getDB("meter");
    }

    @Test
    public void testName() throws Exception {
        Set<String> colls = db.getCollectionNames();

        for (String s : colls) {
            System.out.println(s);
        }

        db.getCollection("meter").drop();

        db.getCollection("meter").insert(new BasicDBObject(createAttributes(123123123123l, "article", 20, 100, 11, 0)));
        db.getCollection("meter").insert(new BasicDBObject(createAttributes(123123123123l, "album", 20, 101, 12, 0)));
        db.getCollection("meter").insert(new BasicDBObject(createAttributes(123123123123l, "video", 30, 201, 13, 0)));
        db.getCollection("meter").insert(new BasicDBObject(createAttributes(123123123123l, "video", 30, 200, 14, 0)));
        db.getCollection("meter").insert(new BasicDBObject(createAttributes(123123123123l, "article", 30, 200, 11, 0)));
        DBCursor result = db.getCollection("meter").find();
        while (result.hasNext()) {
            System.out.println(result.next());
        }

        db.getCollection("meter").update(new BasicDBObject(createAttributes(123123123123l, "article", 20, 100, 11, -1)),
                new BasicDBObject("$inc", new BasicDBObject("$.count", 1)),true, true);

        result = db.getCollection("meter").find();
        while (result.hasNext()) {
            System.out.println(result.next());
        }
    }

    private Map<String, Object> createAttributes(long when, String articleType, int pub, int sec, int art, int cnt) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("when", when);
        attributes.put("articleType", articleType);
        attributes.put("publicationId", pub);
        attributes.put("sectionId", sec);
        attributes.put("articleId", art);
        if (cnt >= 0) {
            attributes.put("count", cnt);
        }
        return attributes;
    }
}
