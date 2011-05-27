package org.semispace.semimeter.dao.mongo;

import com.mongodb.DBCollection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.semispace.semimeter.bean.GroupedResult;
import org.semispace.semimeter.bean.Item;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.PathToken;
import org.semispace.semimeter.bean.TokenizedPathInfo;
import org.semispace.semimeter.bean.mongo.MeterHit;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
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
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(0, semiMeterDao.size());

        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(),
                new MeterHit(12345l, "/article/41/2344/23434433", "/", 3));

        assertEquals(1, semiMeterDao.size());

        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(),
                new MeterHit(12345l, "/article/41/2344/23434433", "/", 3));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(),
                new MeterHit(12345l, "/article/41/2344/23434433", "/", 3));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(),
                new MeterHit(12345l, "/article/41/2344/23434433", "/", 3));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(),
                new MeterHit(12345l, "/article/41/2344/23434433", "/", 3));

        assertEquals(5, semiMeterDao.size());
    }

    @Test
    public void testIsAlive() throws Exception {
        assertTrue(semiMeterDao.isAlive());
    }

    @Test
    public void testSumItems() throws Exception {

        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), new MeterHit(12345l, "/article/1/37/412", "/", 3));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(),
                new MeterHit(12345l, "/album/41/2344/23434433", "/", 4));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(), new MeterHit(12345l, "/article/95/223/4", "/", 5));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(),
                new MeterHit(12345l, "/article/41/2344/23434433", "/", 6));

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

        result = semiMeterDao.sumItems(1, 99999999999l, "/article/*/*/*");
        assertNotNull(result);
        assertEquals(14l, result.longValue());

        result = semiMeterDao.sumItems(1, 99999999999l, "/album");
        assertNotNull(result);
        assertEquals(4l, result.longValue());

        result = semiMeterDao.sumItems(1, 99999999999l, "/article/95");
        assertNotNull(result);
        assertEquals(5l, result.longValue());
    }

    @Test
    @Ignore
    public void testPerformParameterizedQuery() throws Exception {

        Assert.fail("implement test, please");
    }

    @Test
    public void testCreateTimeArray() throws Exception {
        //TODO: extend
        JsonResults[] result = semiMeterDao.createTimeArray("/article/%/%/%", 99999999999l, 0l, 5);
    }

    @Test
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

        mongoTemplate
                .save(mongoTemplate.getDefaultCollectionName(), new MeterHit(1000l, "/article/95/37/411", "/", 1));
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
    @Ignore
    public void testGetHourlySums() throws Exception {

        Assert.fail("implement test, please");
    }

    @Test
    public void testDeleteEntriesOlderThanMillis() throws Exception {
        long current = System.currentTimeMillis();
        long tenSecsAgo = current - 10000;

        mongoTemplate
                .save(mongoTemplate.getDefaultCollectionName(), new MeterHit(tenSecsAgo, "/article/1/37/412", "/", 3));
        mongoTemplate.save(mongoTemplate.getDefaultCollectionName(),
                new MeterHit(current, "/album/41/2344/23434433", "/", 4));

        List<MeterHit> result = mongoTemplate.getCollection(mongoTemplate.getDefaultCollectionName(), MeterHit.class);
        assertEquals(2, result.size());

        semiMeterDao.deleteEntriesOlderThanMillis(5000);

        result = mongoTemplate.getCollection(mongoTemplate.getDefaultCollectionName(), MeterHit.class);
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).getCount().intValue());
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

        List<MeterHit> result = mongoTemplate.getCollection(mongoTemplate.getDefaultCollectionName(), MeterHit.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getCount().intValue());
        assertEquals(4455667788l, result.get(0).getWhen().longValue());
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

        semiMeterDao.performInsertion(items);

        List<MeterHit> result = mongoTemplate.getCollection(mongoTemplate.getDefaultCollectionName(), MeterHit.class);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(3, result.get(0).getCount().intValue());
        assertEquals(4455667788l, result.get(0).getWhen().longValue());

        assertEquals(7, result.get(1).getCount().intValue());
        assertEquals(4455667789l, result.get(1).getWhen().longValue());
    }
}
