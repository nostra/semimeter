package org.semispace.semimeter.service;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import org.semispace.semimeter.bean.JsonResults;
import org.springframework.stereotype.Service;

/**
 * This Service facades json conversion operations.
 * <p/>
 * Date: 3/23/11
 * Time: 10:07 AM
 */
@Service
public class JsonService {

    public String createJsonStringFromArray(JsonResults[] jrs) {
        XStream xStream = new XStream(new JsonHierarchicalStreamDriver());
        xStream.setMode(XStream.NO_REFERENCES);
        xStream.alias("Result", JsonResults.class);
        String str = xStream.toXML(jrs).replaceAll("Result-array", "Results");
        return str;
    }

}
