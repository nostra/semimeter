package org.semispace.semimeter.dao.mongo;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.semispace.semimeter.bean.mongo.MeterHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/context/mongo-test.context.xml"})
public class MongoTest {

    @Autowired
    private MeterRepository meterRepository;


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testName() throws Exception {
        meterRepository.deleteAll();
        
        System.out.println("lala: " + meterRepository.count());

        meterRepository.save(new MeterHit(12345l, "/article/41/2344/23434433", "/", 3));
        
        System.out.println("find: "+        meterRepository.findAll().size());

        System.out.println("lala: " + meterRepository.count());
    }


}
