package org.semispace.semimeter.dao.mongo;

import com.mongodb.Mongo;

/**
 *
 */
public class MongoChecker {
    /**
     * The tests in this class all require a mongodb installation present at localhost:27017.
     * junit's "Assume" mechanism ignores tests when the assume clause fails. we use that here to skip all tests
     * if no mongodb is present.
     *
     * @return true, if local mongodb installation is present.
     */
    public boolean checkMongo() {
        try {
            Mongo mongo = new Mongo("127.0.0.1", 27017);
            mongo.getDatabaseNames();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
