package org.semispace.semimeter.dao.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import org.junit.Before;
import org.junit.Test;
import org.semispace.semimeter.bean.GroupedResult;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.PathToken;
import org.semispace.semimeter.bean.TokenizedPathInfo;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.document.mongodb.MongoTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class SemiMeterDaoMongoTest {

    private SemiMeterDao semiMeterDao;
    private MongoTemplate mongoTemplate;
    private DBCollection coll;

    @Before
    public void before() {
        assumeTrue(checkMongo());

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/context/mongo-test.context.xml");
        this.mongoTemplate = (MongoTemplate) ctx.getBean("mongoTemplate");
        this.semiMeterDao = (SemiMeterDao) ctx.getBean("semimeterDao");

        coll = mongoTemplate.getDefaultCollection();
        coll.drop();
        mongoTemplate.getCollection("sums").drop();
    }

    /**
     * The tests in this class all require a mongodb installation present at localhost:27017.
     * junit's "Assume" mechanism ignores tests when the assume clause fails. we use that here to skip all tests
     * if no mongodb is present.
     *
     * @return true, if local mongodb installation is present.
     */
    private boolean checkMongo() {
        try {
            Mongo mongo = new Mongo("127.0.0.1", 27017);
            mongo.getDatabaseNames();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(0, semiMeterDao.size());

        semiMeterDao.performInsertion(Arrays.asList(
                new Item[]{new Item(1231233l, "/article/1/37/410", 3), new Item(1231233l, "/article/1/37/411", 3),
                        new Item(1231233l, "/article/1/37/412", 3), new Item(1231233l, "/article/1/37/413", 3),
                        new Item(1231233l, "/article/1/37/414", 3)}));

        assertEquals(5, semiMeterDao.size());
    }

    @Test
    public void testIsAlive() throws Exception {
        assertTrue(semiMeterDao.isAlive());
    }


    @Test
    //@Ignore
    public void testGetGroupedSums() throws Exception {

        long now = System.currentTimeMillis();
        final TokenizedPathInfo query = new TokenizedPathInfo("/");
        query.addPathToken(new PathToken(null, "type", false));
        query.addPathToken(new PathToken("95", "publicationId", false));
        query.addPathToken(new PathToken(null, "sectionId", false));
        query.addPathToken(new PathToken(null, "articleId", true));
        semiMeterDao.performInsertion(
                Arrays.asList(new Item[]{new Item(now - 1000 * 60 * 60 * 25, "/article/95/37/411", 1)}));
        List<GroupedResult> result;
        // result = semiMeterDao.getGroupedSums(now - 1000 * 60 * 60 * 24, now, query, 10);
        // assertNotNull(result);
        // assertEquals(0, result.size());

        semiMeterDao.performInsertion(
                Arrays.asList(new Item[]{new Item(now - 1000 * 60 * 60 * 23, "/article/95/37/411", 1)}));
        semiMeterDao.performInsertion(
                Arrays.asList(new Item[]{new Item(now - 1000 * 60 * 60 * 20, "/album/95/2344/412", 4)}));
        semiMeterDao.performInsertion(
                Arrays.asList(new Item[]{new Item(now - 1000 * 60 * 60 * 15, "/article/95/37/413", 2)}));
        semiMeterDao.performInsertion(
                Arrays.asList(new Item[]{new Item(now - 1000 * 60 * 60 * 5, "/article/95/37/413", 3)}));
        semiMeterDao
                .performInsertion(Arrays.asList(new Item[]{new Item(now - 1000 * 60 * 15, "/article/95/37/414", 3)}));

        result = semiMeterDao.getGroupedSums(now - 1000 * 60 * 60 * 20 - 60000, now - 1000 * 60 * 20, query, 10, null);
        System.out.println(result);
        assertNotNull(result);
        //        assertEquals(2, result.size());
        //        assertEquals("413", result.get(0).getKey());
        //        assertEquals(5, result.get(0).getCount());
        //        assertEquals("412", result.get(1).getKey());
        //        assertEquals(4, result.get(1).getCount());

        result = semiMeterDao.getGroupedSums(now - 1000 * 60 * 60 * 20 - 60000, now, query, 3, null);
        assertNotNull(result);
        //        assertEquals(3, result.size());
        //        assertEquals("413", result.get(0).getKey());
        //        assertEquals(5, result.get(0).getCount());
        //        assertEquals("412", result.get(1).getKey());
        //        assertEquals(4, result.get(1).getCount());
        //        assertEquals("414", result.get(2).getKey());
        //        assertEquals(3, result.get(2).getCount());
    }

    @Test
    public void testGetHourlySumsTotal() throws Exception {

        long now = System.currentTimeMillis();
        semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(now, "/article/1/37/410", 3)}));
        semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(now, "/article/95/2342/123432", 5)}));

        List<GroupedResult> result = semiMeterDao.getHourlySums(null, null);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(8, result.get(0).getCount());
    }

    @Test
    public void testGetHourlySumsPublication() throws Exception {

        long now = System.currentTimeMillis();
        semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(now, "/article/1/37/410", 3)}));
        semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(now, "/article/95/2342/123432", 5)}));

        List<GroupedResult> result = semiMeterDao.getHourlySums(1, null);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getCount());
    }

    @Test
    public void testDeleteNoEntry() throws Exception {
        long current = System.currentTimeMillis();
        long oldmillis = current - 1000 * 60 * 5;

        semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(oldmillis, "/article/1/37/412", 3)}));

        List<DBObject> result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(1, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 6);

        result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        DBObject doc = result.get(0);
        assertEquals(412, doc.get("id"));
        DBObject day = (DBObject) doc.get("day");
        assertEquals(3, day.get("count"));
        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(1, result.size());
    }

    @Test
    public void testDeleteOneEntry() throws Exception {
        long current = System.currentTimeMillis();
        long oldmillis = current - 1000 * 60 * 5 + 1;

        semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(oldmillis, "/article/1/37/412", 3)}));

        List<DBObject> result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        assertEquals(1, ((BasicDBObject) ((DBObject) result.get(0).get("day")).get("hours")).size());
        assertEquals(3, ((DBObject) result.get(0).get("day")).get("count"));
        assertEquals(3, ((DBObject) result.get(0).get("day")).get("last15minutes"));
        assertEquals(3, ((DBObject) result.get(0).get("day")).get("last180minutes"));
        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(1, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 5);
        //after first run, article should be empty, but there
        result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        assertEquals(0, ((BasicDBObject) ((DBObject) result.get(0).get("day")).get("hours")).size());
        assertEquals(0, ((DBObject) result.get(0).get("day")).get("count"));
        assertEquals(0, ((DBObject) result.get(0).get("day")).get("last15minutes"));
        assertEquals(0, ((DBObject) result.get(0).get("day")).get("last180minutes"));
        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(0, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 5);

        //now article should be gone
        result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(0, result.size());
        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(0, result.size());
    }

    @Test
    public void testDeleteEntriesOlderThanMillis() throws Exception {
        long current = System.currentTimeMillis();
        long oldmillis = current - 1000 * 60 * 5;

        semiMeterDao.performInsertion(Arrays.asList(
                new Item[]{new Item(oldmillis, "/article/1/37/412", 3), new Item(current, "/article/1/37/412", 4)}));

        List<DBObject> result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        DBObject doc = result.get(0);
        assertEquals(412, doc.get("id"));
        DBObject day = (DBObject) doc.get("day");
        assertEquals(7, day.get("count"));
        assertEquals(7, day.get("last15minutes"));
        assertEquals(7, day.get("last180minutes"));
        DBObject hours = (DBObject) day.get("hours");
        DBObject hour = (DBObject) hours.get(hours.keySet().iterator().next());
        //this will fail at the beginning of each hour, hence commented out
        //assertEquals(7, hour.get("count"));
        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 4);

        result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        assertEquals(412, result.get(0).get("id"));
        assertEquals(1, result.size());
        doc = result.get(0);
        assertEquals(412, doc.get("id"));
        day = (DBObject) doc.get("day");
        assertEquals(4, day.get("count"));
        assertEquals(4, day.get("last15minutes"));
        assertEquals(4, day.get("last180minutes"));
        hours = (DBObject) day.get("hours");
        hour = (DBObject) hours.get(hours.keySet().iterator().next());
        //this will fail at the beginning of each hour, hence commented out
        //assertEquals(4, hour.get("count"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).get("total"));
    }

    @Test
    public void testLatest15_1() throws Exception {
        long current = System.currentTimeMillis();
        long oldmillis = current - 1000 * 60 * 14;

        semiMeterDao.performInsertion(Arrays.asList(
                new Item[]{new Item(oldmillis, "/article/1/37/412", 3), new Item(current, "/article/1/37/412", 4)}));

        List<DBObject> result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        DBObject doc = result.get(0);
        assertEquals(412, doc.get("id"));
        DBObject day = (DBObject) doc.get("day");
        assertEquals(7, day.get("count"));
        assertEquals(7, day.get("last15minutes"));
        assertEquals(7, day.get("last180minutes"));
        DBObject hours = (DBObject) day.get("hours");
        DBObject hour = (DBObject) hours.get(hours.keySet().iterator().next());
        //this will fail at the beginning of each hour, hence commented out
        //assertEquals(7, hour.get("count"));
        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 60* 24);

        result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        assertEquals(412, result.get(0).get("id"));
        assertEquals(1, result.size());
        doc = result.get(0);
        assertEquals(412, doc.get("id"));
        day = (DBObject) doc.get("day");
        assertEquals(7, day.get("count"));
        assertEquals(7, day.get("last15minutes"));
        assertEquals(7, day.get("last180minutes"));
        hours = (DBObject) day.get("hours");
        hour = (DBObject) hours.get(hours.keySet().iterator().next());
        //this will fail at the beginning of each hour, hence commented out
        //assertEquals(4, hour.get("count"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());
    }

    @Test
    public void testLatest15_2() throws Exception {
        long current = System.currentTimeMillis();
        long oldmillis = current - 1000 * 60 * 15;

        semiMeterDao.performInsertion(Arrays.asList(
                new Item[]{new Item(oldmillis, "/article/1/37/412", 3), new Item(current, "/article/1/37/412", 4)}));

        List<DBObject> result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        DBObject doc = result.get(0);
        assertEquals(412, doc.get("id"));
        DBObject day = (DBObject) doc.get("day");
        assertEquals(7, day.get("count"));
        assertEquals(7, day.get("last15minutes"));
        assertEquals(7, day.get("last180minutes"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 60 * 24);

        result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        assertEquals(412, result.get(0).get("id"));
        assertEquals(1, result.size());
        doc = result.get(0);
        assertEquals(412, doc.get("id"));
        day = (DBObject) doc.get("day");
        assertEquals(7, day.get("count"));
        assertEquals(4, day.get("last15minutes"));
        assertEquals(7, day.get("last180minutes"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());
    }

    @Test
    public void testLatest180_1() throws Exception {
        long current = System.currentTimeMillis();
        long oldmillis = current - 1000 * 60 * 179;

        semiMeterDao.performInsertion(Arrays.asList(
                new Item[]{new Item(oldmillis, "/article/1/37/412", 3), new Item(current - 1000*60*20, "/article/1/37/412", 4)}));

        List<DBObject> result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        DBObject doc = result.get(0);
        assertEquals(412, doc.get("id"));
        DBObject day = (DBObject) doc.get("day");
        assertEquals(7, day.get("count"));
        assertEquals(7, day.get("last15minutes"));
        assertEquals(7, day.get("last180minutes"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 60 * 24);

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());

        result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        assertEquals(412, result.get(0).get("id"));
        assertEquals(1, result.size());
        doc = result.get(0);
        assertEquals(412, doc.get("id"));
        day = (DBObject) doc.get("day");

        assertEquals(7, day.get("count"));
        assertEquals(0, day.get("last15minutes"));
        assertEquals(7, day.get("last180minutes"));
    }

    @Test
    public void testLatest180_2() throws Exception {
        long current = System.currentTimeMillis();
        long oldmillis = current - 1000 * 60 * 180 ;

        semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(oldmillis, "/article/1/37/412", 3),
                new Item(current - 1000 * 60 * 20, "/article/1/37/412", 4)}));

        List<DBObject> result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        DBObject doc = result.get(0);
        assertEquals(412, doc.get("id"));
        DBObject day = (DBObject) doc.get("day");
        assertEquals(7, day.get("count"));
        assertEquals(7, day.get("last15minutes"));
        assertEquals(7, day.get("last180minutes"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 60 * 24);

        result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        assertEquals(412, result.get(0).get("id"));
        assertEquals(1, result.size());
        doc = result.get(0);
        assertEquals(412, doc.get("id"));
        day = (DBObject) doc.get("day");
        assertEquals(7, day.get("count"));
        assertEquals(0, day.get("last15minutes"));
        assertEquals(4, day.get("last180minutes"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(2, result.size());
    }

    @Test
    public void testPerformInsertionOneItem() throws Exception {
        final Collection<Item> items = new ArrayList<Item>();
        Item item = new Item();
        item.setAccessNumber(3);
        item.setPath("/video/1/37/412");
        item.setWhen(4455667788l);
        items.add(item);

        semiMeterDao.performInsertion(items);
        DBCursor dbResult = mongoTemplate.getDefaultCollection().find();
        assertNotNull(dbResult);
        List<DBObject> dbList = dbResult.toArray();
        assertEquals(1, dbList.size());

        dbResult = mongoTemplate.getCollection("sums").find();
        assertNotNull(dbResult);
        dbList = dbResult.toArray();
        assertEquals(1, dbList.size());

    }

    @Test
    public void testPerformInsertionMultipleItems() throws Exception {
        final Collection<Item> items = new ArrayList<Item>();
        Item item = new Item();
        item.setAccessNumber(3);
        item.setPath("/video/1/37/412");
        item.setWhen(4455667788l);
        items.add(item);

        item = new Item();
        item.setAccessNumber(7);
        item.setPath("/article/1/37/411");
        item.setWhen(4455667789l);
        items.add(item);

        item = new Item();
        item.setAccessNumber(7);
        item.setPath("/article/1/37/411");
        item.setWhen(4455767789l);
        items.add(item);

        semiMeterDao.performInsertion(items);

        List<DBObject> result = mongoTemplate.getCollection(mongoTemplate.getDefaultCollectionName()).find().toArray();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(3, ((DBObject) result.get(0).get("day")).get("count"));
        assertEquals(412, result.get(0).get("id"));

        assertEquals(14, ((DBObject) result.get(1).get("day")).get("count"));
        assertEquals(411, result.get(1).get("id"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        System.out.println(result);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10, result.get(0).get("total"));
        assertEquals(7, result.get(1).get("total"));
    }
}
