package org.semispace.semimeter;

import com.mongodb.ServerAddress;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public abstract class JndiHelper {
    public static List<ServerAddress> arrayToList(String[] array) throws UnknownHostException {
        System.out.println("incoming: "+array);


        List<ServerAddress> result = new ArrayList<ServerAddress>();
        for (String s : array) {
            String[] split = s.split(":");
            String host = split[0];
            int port = Integer.valueOf(split[1]);
            result.add(new ServerAddress(host, port));
        }
        System.out.println("\n\n\n\noutgoing: "+result);
        return result;

    }
}
