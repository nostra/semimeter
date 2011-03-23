package org.semispace.semimeter.service;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.ArrayQuery;
import org.semispace.semimeter.bean.ArrayQueryResult;
import org.semispace.semimeter.bean.DisplayIntent;
import org.semispace.semimeter.bean.DisplayResult;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.bean.ParameterizedQuery;
import org.semispace.semimeter.bean.ParameterizedQueryResult;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This service facades all space and dao operations, so that web controllers won't have to
 * be coupled to semispace and/or particular DAOs
 * <p/>
 * Date: 3/23/11
 * Time: 10:04 AM
 */
@Service
public class SemimeterService {
    private final Logger log = LoggerFactory.getLogger(SemimeterService.class);
    private static final long QUERY_LIFE_TIME_MS = 5000;
    private static final long QUERY_RESULT_TIMEOUT_MS = 6000;

    @Autowired
    private JsonService jsonService;

    @Autowired
    private SemiMeterDao semiMeterDao;

    private SemiSpaceInterface space = SemiSpace.retrieveSpace();

    public long getSummedItemsFromDb(final String path, final long endAt, final long startAt, final long previousPeriod) {
        return semiMeterDao.sumItems(startAt, endAt, path + "%").longValue() - semiMeterDao.sumItems(previousPeriod, startAt, path + "%").longValue();
    }

    public String getCurrentCount(final String path, final String resolution, final long endAt, final long startAt) {
        String result = null;
        DisplayIntent di = new DisplayIntent(path + "_" + resolution);
        DisplayResult dr = space.readIfExists(new DisplayResult(di.getPath()));
        if (dr == null && space.readIfExists(di) == null) {
            space.write(di, QUERY_LIFE_TIME_MS);

            Long count = semiMeterDao.sumItems(startAt, endAt, path + "%");
            JsonResults[] jrs = new JsonResults[1];
            jrs[0] = new JsonResults();
            jrs[0].setKey("show");
            jrs[0].setValue("" + count);
            String str = jsonService.createJsonStringFromArray(jrs);

            dr = new DisplayResult(di.getPath());
            dr.setResult(str);
            space.write(dr, QUERY_RESULT_TIMEOUT_MS);
            space.takeIfExists(di);
        }
        if (dr == null) {
            // DisplayIntent was present
            dr = space.read(dr, QUERY_LIFE_TIME_MS);
        }
        if (dr != null) {
            result = dr.getResult();
        }
        return result;
    }

    public JsonResults[] getJsonResults(final String path, final long endAt, final long startAt, final String resolution) {
        ParameterizedQuery pq = new ParameterizedQuery(resolution, startAt, endAt, path);
        ParameterizedQueryResult toFind = new ParameterizedQueryResult(pq.getKey(), null);
        ParameterizedQueryResult pqr = space.readIfExists(toFind);
        if (pqr == null) {
            //log.debug("No previous result for {}", pq.getKey());
            if (space.readIfExists(pq) == null) {
                space.write(pq, QUERY_LIFE_TIME_MS);
            } else {
                log.debug("Query for {} has already been placed", pq.getKey());
            }
            pqr = space.read(toFind, QUERY_RESULT_TIMEOUT_MS);
        } else {
            log.trace("Using existing result for {}", pq.getKey());
        }
        JsonResults[] jrs = null;
        if (pqr != null) {
            jrs = pqr.getResults();
        } else {
            log.debug("Query timed out: {}", pq.getKey());
        }
        return jrs;
    }

    public JsonResults[] getArrayCounts(final String resolution, final Integer numberOfSamples, final String path, final long endAt, final long startAt) {
        ArrayQuery aq = new ArrayQuery(resolution, startAt, endAt, path + "%", numberOfSamples);
        ArrayQueryResult toFind = new ArrayQueryResult(aq.getKey(), null);
        ArrayQueryResult aqr = space.readIfExists(toFind);
        if (aqr == null) {
            //log.debug("No previous result for {}", aq.getKey());
            if (space.readIfExists(aq) == null) {
                space.write(aq, QUERY_LIFE_TIME_MS);
            } else {
                log.debug("Query for {} has already been placed", aq.getKey());
            }
            aqr = space.read(toFind, QUERY_RESULT_TIMEOUT_MS);
        } else {
            log.trace("Using existing result for {}", aq.getKey());
        }
        JsonResults[] jrs = null;
        if (aqr != null) {
            jrs = aqr.getResults();
        } else {
            log.debug("ArrayQuery timed out: {}", aq.getKey());
        }
        return jrs;
    }
}
