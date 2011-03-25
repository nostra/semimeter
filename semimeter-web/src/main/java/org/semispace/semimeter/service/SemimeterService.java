package org.semispace.semimeter.service;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.*;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Default query skew is 20 seconds. This is in order to let the database have
     * time to insert pending data.
     * TODO Consider adjusting the value.
     */
    private static final long DEFAULT_SKEW_IN_MS = 20000;
    private static final long QUERY_LIFE_TIME_MS = 5000;
    private static final long QUERY_RESULT_TIMEOUT_MS = 6000;

    @Autowired
    private JsonService jsonService;

    @Autowired
    private SemiMeterDao semiMeterDao;

    private SemiSpaceInterface space = SemiSpace.retrieveSpace();

    /**
     * @param path           either one complete path or just the beginning of a number of grouped paths. Example: if
     *                       the path syntax is [category]/[subCategory]/[itemNumber], then the following would be
     *                       valid path parameters for this method:
     *                       <ul>
     *                       <li>books/adventure/2121  (one complete path)</li>
     *                       <li>books                 (grouped category)</li>
     *                       <li>books/adventure       (grouped sub-category)</li>
     *                       </ul>
     * @param endAt          a long value. take all events counted up to this timestamp into calculation. typically
     *                       "now"
     * @param startAt        a long value. take all events counted after this timestamp into calculation. typically "a
     *                       day before now" or "an hour before now"
     * @param previousPeriod a long value. marks the beginning of the reference period. the reference period starts at
     *                       <code>previousPeriod</code> and ends at <code>startAt</code>
     * @return a delta between the counted views of two succeeding periods of time.
     *         The reference period is between <code>previousPeriod</code> and <code>startAt</code>. All counts from
     *         this period will be subtracted from the "current" period, which is between <code>startAt</code> and
     *         <code>endAt</code>.
     *         The subtraction result will be returned.
     */
    public long getDeltaFromDb(final String path, final long endAt, final long startAt, final long previousPeriod) {
        return semiMeterDao.sumItems(startAt, endAt, path + "%").longValue() - semiMeterDao.sumItems(previousPeriod, startAt, path + "%").longValue();
    }

    /**
     * Fetches count data. This method makes direct use of a DAO, the space is used though to avoid concurrent requests to
     * the same data.
     *
     * @param path       either one complete path or just the beginning of a number of grouped paths. Example: if
     *                   the path syntax is [category]/[subCategory]/[itemNumber], then the following would be
     *                   valid path parameters for this method:
     *                   <ul>
     *                   <li>books/adventure/2121  (one complete path)</li>
     *                   <li>books                 (grouped category)</li>
     *                   <li>books/adventure       (grouped sub-category)</li>
     *                   </ul>
     * @param resolution a string describing the period of time that shall be used for the calculation. valid values
     *                   are:
     *                   <ul>
     *                   <li>second</li>
     *                   <li>minute</li>
     *                   <li>hour</li>
     *                   <li>day</li>
     *                   <li>week</li>
     *                   <li>month</li>
     *                   <li>total</li>
     *                   </ul>
     * @param endAt      a long value. take all events counted up to this timestamp into calculation. typically
     *                   "now"
     * @param startAt    a long value. take all events counted after this timestamp into calculation. typically "a
     *                   day before now" or "an hour before now"
     * @return sum of counted views for given path expression and given period of time
     */
    public Long getCurrentCount(final String path, final String resolution, final long endAt, final long startAt) {
        Long result = null;
        DisplayIntent di = new DisplayIntent(path + "_" + resolution);
        DisplayResult dr = space.readIfExists(new DisplayResult(di.getKey()));
        if (dr == null && space.readIfExists(di) == null) {
            space.write(di, QUERY_LIFE_TIME_MS);

            Long count = semiMeterDao.sumItems(startAt, endAt, path + "%");

            dr = new DisplayResult(di.getKey());
            dr.setResult(count);
            space.write(dr, QUERY_RESULT_TIMEOUT_MS);
            space.takeIfExists(di);
        }
        if (dr == null) {
            // DisplayIntent was present
            dr = space.read(dr, QUERY_LIFE_TIME_MS);
        }
        if (dr != null) {
            result = (Long) dr.getResult();
        }
        return result;
    }

    /**
     * Fetches countdata.
     * A request-bean is stored in the space, and SpacePQListener picks up the request bean.
     * SpacePQListener will then handle the request and put a resultBean into the space when done.
     * Meanwhile, the current method will be waiting up to 6 seconds for the result bean (useing semispace's timeout
     * feature on space operations)
     *
     * @param path       for this method, the path parameter MUST contain one dollar ('$') character. The dollar is a
     *                   wildcard, but it can be appended to complete pathes aswell. Example: if
     *                   the path syntax is [category]/[subCategory]/[itemNumber], then the following would be
     *                   valid path parameters for this method:
     *                   <ul>
     *                   <li>books/adventure/2121/$  (one complete path)</li>
     *                   <li>books/$                 (all items in books category)</li>
     *                   <li>books/adventure/$       (all items in book category and adventure subcat)</li>
     *                   <li>books/$/2121            (all books with concrete itemId, grouped by subcategory)</li>
     *                   <li>$/adventure/2121        (all items with sub-cat adventure and item id 2121, grouped by cat)</li>
     *                   </ul>
     * @param endAt      a long value. take all events counted up to this timestamp into calculation. typically "now"
     * @param startAt    a long value. take all events counted after this timestamp into calculation. typically "a
     *                   day before now" or "an hour before now"
     * @param resolution a string describing the period of time that shall be used for the calculation. valid values
     *                   are:
     *                   <ul>
     *                   <li>second</li>
     *                   <li>minute</li>
     *                   <li>hour</li>
     *                   <li>day</li>
     *                   <li>week</li>
     *                   <li>month</li>
     *                   <li>total</li>
     *                   </ul>
     * @return returns an array of JsonResults. Each JsonResult has a key, which is the wildcard value that the dollar
     *         in the given path variable matched with. the JsonResult value is the number of counts for that match in
     *         the given period of time.
     */
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

    public List<GroupedResult> getOrderedResults(final TokenizedPathInfo query, final long startAt, final long endAt, final String resolution, final int maxResults) {
        List<GroupedResult> result = new ArrayList<GroupedResult>();
        String path = query.buildPathFromTokens();
        log.debug("path: {}", path);
        DisplayIntent di = new DisplayIntent(path + "_" + resolution + "_" + maxResults);
        DisplayResult dr = space.readIfExists(new DisplayResult(di.getKey()));
        if (dr == null && space.readIfExists(di) == null) {
            space.write(di, QUERY_LIFE_TIME_MS);

            List<GroupedResult> resultList = null;
            try {
                resultList = semiMeterDao.getGroupedSums(startAt, endAt, query, maxResults);
            } catch (IllegalArgumentException e) {
                log.error("invalid query parameter", e);
            }

            dr = new DisplayResult(di.getKey());
            dr.setResult(resultList);
            space.write(dr, QUERY_RESULT_TIMEOUT_MS);
            space.takeIfExists(di);
        }
        if (dr == null) {
            // DisplayIntent was present
            dr = space.read(dr, QUERY_LIFE_TIME_MS);
        }
        if (dr != null && dr.getResult() != null) {
            return (List<GroupedResult>) dr.getResult();
        }
        return result;
    }


    /**
     * Calculates starting point as (in milliseconds).
     *
     * @param resolution String indicating how long before <code>endAt</code> the starting point shall be. Valid values are
     *                   <ul>
     *                   <li>second</li>
     *                   <li>minute</li>
     *                   <li>hour</li>
     *                   <li>day</li>
     *                   <li>week</li>
     *                   <li>month</li>
     *                   <li>total</li>
     *                   </ul>
     * @param endAt      base value for the calculation. the result will be  {whateverresolutionyouchose} before this point in time.
     * @return a point in time, as milliseconds since 1970/01/01
     */
    public long calculateStartTimeFromResolution(String resolution, long endAt) {
        long startAt;
        if (resolution.equalsIgnoreCase("second")) {
            startAt = endAt - 1000;
        } else if (resolution.equalsIgnoreCase("minute")) {
            startAt = endAt - 60000;
        } else if (resolution.equalsIgnoreCase("hour")) {
            startAt = endAt - 60000 * 60;
        } else if (resolution.equalsIgnoreCase("day")) {
            startAt = endAt - 60000 * 60 * 24;
        } else if (resolution.equalsIgnoreCase("week")) {
            startAt = endAt - 60000 * 60 * 24 * 7;
        } else if (resolution.equalsIgnoreCase("month")) {
            // Using 30 day month
            startAt = endAt - 60000 * 60 * 24 * 7 * 30l;
        } else if (resolution.equalsIgnoreCase("total")) {
            // Defaulting to total - beginning at time 0
            startAt = 0;
        } else {
            throw new RuntimeException("Did not understand resolution " + resolution);
        }
        return startAt;
    }

    /**
     * @return the soonest possible end point for a calculation period.
     */
    public long getCurrentEndTime() {
        return System.currentTimeMillis() - DEFAULT_SKEW_IN_MS;
    }

    public long calculateNumberOfSamples(String resolution) {
        long numberOfSamples;
        if (resolution.equalsIgnoreCase("second")) {
            numberOfSamples = 60; // A resolution of seconds does not make sense.
        } else if (resolution.equalsIgnoreCase("minute")) {
            numberOfSamples = 60;
        } else if (resolution.equalsIgnoreCase("hour")) {
            numberOfSamples = 60;
        } else if (resolution.equalsIgnoreCase("day")) {
            numberOfSamples = 24;
        } else if (resolution.equalsIgnoreCase("week")) {
            numberOfSamples = 7;
        } else if (resolution.equalsIgnoreCase("month")) {
            // Using 30 day month
            numberOfSamples = 30;
        } else if (resolution.equalsIgnoreCase("total")) {
            // Defaulting to total - beginning at time 0
            numberOfSamples = 10;
        } else {
            throw new RuntimeException("Did not understand resolution " + resolution);
        }
        return numberOfSamples;
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
