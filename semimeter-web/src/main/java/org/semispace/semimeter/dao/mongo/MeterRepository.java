package org.semispace.semimeter.dao.mongo;

import org.semispace.semimeter.bean.mongo.MeterHit;
import org.springframework.data.document.mongodb.repository.MongoRepository;

/**
 * This interface defines a mongodb repository (it will be proxy-implemented automatically at runtime)
 */
public interface MeterRepository extends MongoRepository<MeterHit, String> {
}
