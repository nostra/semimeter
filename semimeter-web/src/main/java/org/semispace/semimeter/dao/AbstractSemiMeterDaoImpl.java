package org.semispace.semimeter.dao;

import org.semispace.semimeter.bean.JsonResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractSemiMeterDaoImpl implements SemiMeterDao {
    private static final Logger log = LoggerFactory.getLogger(AbstractSemiMeterDaoImpl.class);

    /**
     * protected for the benefit of junit test
     */
    protected List<JsonResults> flatten(List<Map<String, Object>> list, int num) {
        // Using AtomicInteger just because it has nice adding features.
        List<AtomicInteger> res = new ArrayList<AtomicInteger>(num);
        for (int i = 0; i < num; i++) {
            // Prime
            res.add(new AtomicInteger());
        }
        if (list == null || list.isEmpty()) {
            return transformToJR(res);
        }
        long start = Long.valueOf("" + list.get(0).get("updated")).longValue();
        long end = Long.valueOf("" + list.get(list.size() - 1).get("updated")).longValue();
        long subrange = (end - start) / num;
        if (subrange < 1) {
            subrange = 1;
        }
        long mod = (end - start) / subrange;
        if (mod > 0) {
            log.trace("Having a rest which I need to retain. Just adding it to subrange. Rest is " + mod);
            subrange += mod;
        }
        int count = 0;
        for (int i = 0; i < num; i++) {
            start += subrange;
            //log.debug("Starting at "+start+" at iteration "+i+" having subrange "+subrange);
            // Still having more data, and current updated value less than where to start in scale
            while (count < list.size() && Long.valueOf("" + list.get(count).get("updated")).longValue() <= start) {
                //log.debug("Shall increment place "+i);
                res.get(i).getAndAdd(Integer.valueOf("" + list.get(count).get("counted")).intValue());
                count++;
            }
        }
        if (count < list.size()) {
            log.error(
                    "Sanity: Did not use all data!? This may happen if the data are not sorted in the updated field. Missing " +
                            (list.size() - count) + " elements");
        }

        return transformToJR(res);
    }

    private List<JsonResults> transformToJR(List<AtomicInteger> res) {
        List<JsonResults> list = new ArrayList<JsonResults>();
        for (int i = 0; i < res.size(); i++) {
            JsonResults jr = new JsonResults();
            jr.setKey("" + i);
            jr.setValue(res.get(i).toString());
            list.add(jr);
        }
        return list;
    }
}
