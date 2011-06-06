package org.semispace.semimeter.dao.mongo;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.semispace.semimeter.bean.GroupedResult;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.PathToken;
import org.semispace.semimeter.bean.TokenizedPathInfo;
import org.semispace.semimeter.bean.mongo.MeterHit;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sun.nio.cs.ext.DBCS_ONLY_IBM_EBCDIC_Decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/context/mongo-test.context.xml"})
public class SemiMeterDaoMongoTest {

    @Autowired
    private SemiMeterDao semiMeterDao;
    @Autowired
    private MongoTemplate mongoTemplate;
    private DBCollection coll;

    @Before
    public void before() {
        coll = mongoTemplate.getDefaultCollection();
        coll.drop();
        mongoTemplate.getCollection("sums").drop();
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
    @Ignore
    public void testGetGroupedSums() throws Exception {

        final TokenizedPathInfo query = new TokenizedPathInfo("/");
        query.addPathToken(new PathToken(null, "type", false));
        query.addPathToken(new PathToken("95", "publicationId", false));
        query.addPathToken(new PathToken(null, "sectionId", false));
        query.addPathToken(new PathToken(null, "articleId", true));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), new MeterHit(1l, "/dummy/95/37/411", "/", 1));
        List<GroupedResult> result = semiMeterDao.getGroupedSums(1000, 5000, query, 10);
        assertNotNull(result);
        assertEquals(0, result.size());

        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), new MeterHit(1000l, "/article/95/37/411", "/", 1));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), new MeterHit(1001l, "/album/95/2344/412", "/", 4));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), new MeterHit(2000l, "/article/95/37/413", "/", 2));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), new MeterHit(5000l, "/article/95/37/413", "/", 3));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), new MeterHit(5001l, "/article/95/37/414", "/", 3));
        result = semiMeterDao.getGroupedSums(1000, 5000, query, 10);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("413", result.get(0).getKey());
        assertEquals(5, result.get(0).getCount());
        assertEquals("412", result.get(1).getKey());
        assertEquals(4, result.get(1).getCount());

        result = semiMeterDao.getGroupedSums(999, 5001, query, 3);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("413", result.get(0).getKey());
        assertEquals(5, result.get(0).getCount());
        assertEquals("412", result.get(1).getKey());
        assertEquals(4, result.get(1).getCount());
        assertEquals("414", result.get(2).getKey());
        assertEquals(3, result.get(2).getCount());
    }

    @Test
    public void testGetHourlySumsTotal() throws Exception {

        long now = System.currentTimeMillis();
         semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(now, "/article/1/37/410", 3)}));

        List<GroupedResult> result = semiMeterDao.getHourlySums(null, null);
        assertNotNull(result);
    }

        @Test
    public void testGetHourlySumsPublication() throws Exception {

        long now = System.currentTimeMillis();
         semiMeterDao.performInsertion(Arrays.asList(new Item[]{new Item(now, "/article/1/37/410", 3)}));

        List<GroupedResult> result = semiMeterDao.getHourlySums(1, null);
        assertNotNull(result);
    }

    @Test
    public void testDeleteNoEntry() throws Exception {
        long current = System.currentTimeMillis();
        long oldmillis = current - 1000 * 60 * 5;

        semiMeterDao.performInsertion(Arrays.asList(
                new Item[]{new Item(oldmillis, "/article/1/37/412", 3)}));

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

        semiMeterDao.performInsertion(Arrays.asList(
                new Item[]{new Item(oldmillis, "/article/1/37/412", 3)}));

        List<DBObject> result = mongoTemplate.getDefaultCollection().find().toArray();
        assertEquals(1, result.size());
        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(1, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(1000 * 60 * 5);

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
        hours = (DBObject) day.get("hours");
        hour = (DBObject) hours.get(hours.keySet().iterator().next());
        //this will fail at the beginning of each hour, hence commented out
        //assertEquals(4, hour.get("count"));

        result = mongoTemplate.getCollection("sums").find().toArray();
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).get("total"));
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


    /*
    @Test
    public void testSumItems() throws Exception {
    semiMeterDao.performInsertion(Arrays.asList(
            new Item[]{new Item(12345l, "/article/1/37/412", 3), new Item(12345l, "/album/41/2344/23434433", 4),
                    new Item(12345l, "/article/95/223/4", 5), new Item(12345l, "/article/41/2344/23434433", 6)}));

    Long result = semiMeterDao.sumItems(1, 99999999999l, null);
    assertNotNull(result);
    assertEquals(18l, result.longValue());

    result = semiMeterDao.sumItems(1, 99999999999l, "");
    assertNotNull(result);
    assertEquals(18l, result.longValue());

    result = semiMeterDao.sumItems(1, 99999999999l, "/");
    assertNotNull(result);
    assertEquals(18l, result.longValue());

    result = semiMeterDao.sumItems(1, 99999999999l, "/lalala");
    assertNotNull(result);
    assertEquals(0l, result.longValue());

    result = semiMeterDao.sumItems(1, 99999999999l, "/article");
    assertNotNull(result);
    assertEquals(14l, result.longValue());

    result = semiMeterDao.sumItems(1, 99999999999l, "/article/_/_/_");
    assertNotNull(result);
    assertEquals(14l, result.longValue());

    result = semiMeterDao.sumItems(1, 99999999999l, "/article/%/%/%");
    assertNotNull(result);
    assertEquals(14l, result.longValue());
    */
    //result = semiMeterDao.sumItems(1, 99999999999l, "/article/*/*/*");
    /*
        assertNotNull(result);
        assertEquals(14l, result.longValue());

        result = semiMeterDao.sumItems(1, 99999999999l, "/album");
        assertNotNull(result);
        assertEquals(4l, result.longValue());

        result = semiMeterDao.sumItems(1, 99999999999l, "/article/95");
        assertNotNull(result);
        assertEquals(5l, result.longValue());
    }
/*
    @Test
    public void testPerformParameterizedQuery() throws Exception {

        Assert.fail("implement test, please");
    }

    @Test
    public void testCreateTimeArray() throws Exception {
        //TODO: extend
        JsonResults[] result = semiMeterDao.createTimeArray("/article/%/%/%", 99999999999l, 0l, 5);
    }
*/
}
