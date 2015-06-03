package org.semispace.semimeter;

import com.mongodb.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class JndiHelper {
    private static final Logger log = LoggerFactory.getLogger(JndiHelper.class);

    public static List<ServerAddress> arrayToList(String[] array) throws UnknownHostException {
        log.debug("incoming: {}", Arrays.asList(array));

        List<ServerAddress> result = new ArrayList<ServerAddress>();
        for (String s : array) {
            String[] split = s.split(":");
            String host = split[0];
            int port = Integer.valueOf(split[1]);
            result.add(new ServerAddress(host, port));
        }
        log.debug("outgoing: {}", Arrays.asList(result));
        return result;
    }
}
