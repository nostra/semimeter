/*
 * Copyright 2009 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semispace.semimeter.controller;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.semispace.semimeter.space.CounterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CounterController {
    private final Logger log = LoggerFactory.getLogger(CounterController.class);

    @Autowired
    private SemiMeterDao semiMeterDao;
    /**
     * Default query skew is 30 seconds. This is in order to let the database have
     * time to insert pending data.
     * TODO Consider adjusting the value.
     */
    private static final long DEFAULT_SKEW_IN_MS = 30000;


    @RequestMapping("/index.html")
    public String entryPage() {
        return "index";
    }


    /**
     * Queries on graph strongly correlates to queries on json.html, URI-wise
     */
    @RequestMapping("**/graph.html")
    public String graphPage( Model model,HttpServletRequest request, @RequestParam String resolution ) {
        long endAt = System.currentTimeMillis() - DEFAULT_SKEW_IN_MS;
        long startAt = calculateStartTimeFromResolution(resolution, endAt);

        JsonResults[] jrs = createJsonResults(request.getServletPath(), endAt, startAt, "/graph.html");
        long max = 0;
        for ( JsonResults jr :jrs ) {
            max = Math.max( max, Long.valueOf(jr.getValue()).longValue());
        }
        max++;
        max *= 1.25;

        model.addAttribute("resolution", resolution);
        model.addAttribute("xAxisSize", Long.valueOf(max));

        long updtFreq;
        if ( resolution.equalsIgnoreCase("second")) {
            updtFreq = 2000;
        } else if ( resolution.equalsIgnoreCase("minute")) {
            updtFreq = 30000;
        } else {
            // Default to every minute
            updtFreq = 60000;
        }
        String res = System.getProperty(CounterHolder.RESOLUTION_MS_SYSTEM_VARIABLE);
        if ( res != null ) {
            updtFreq = Math.max( updtFreq, 2*Long.valueOf(res));
        }

        model.addAttribute("updateInterval", updtFreq);

        return "bargraph";
    }

    @RequestMapping("**/json.html")
    public String showData( Model model, HttpServletRequest request, @RequestParam String resolution ) {
        /*log.debug("--------------- *-PathTranslated: "+request.getPathTranslated()+
                "\nContextPath: "+request.getContextPath()+
                "\nPathInfo: "+request.getPathInfo()+
                "\nRequestURI: "+request.getRequestURI()+
                "\nServletPath(): "+request.getServletPath()+
                "\nRequestURL(): "+request.getRequestURL()
        );*/
        //Seems like ServletPath() is the way to go.
        // http://localhost:9013/semimeter/semimeter/a/json.html
        // ServletPath(): /semimeter/a/json.html

        long endAt = System.currentTimeMillis() - DEFAULT_SKEW_IN_MS;
        long startAt = calculateStartTimeFromResolution(resolution, endAt);

        JsonResults[] jrs = createJsonResults(request.getServletPath(), endAt, startAt, "/json.html");

        XStream xStream = new XStream(new JsonHierarchicalStreamDriver());
        xStream.setMode(XStream.NO_REFERENCES);
        xStream.alias("Result", JsonResults.class);
        // Easiest way of getting rid of the -array suffix.
        String str = xStream.toXML(jrs).replaceAll("Result-array", "Results");

        model.addAttribute("numberOfItems", str);

        return "showcount";
    }

    /**
     * TODO Perform query via semispace
     */    
    private JsonResults[] createJsonResults(String spath, long endAt, long startAt, String toTrim ) {
        String path = spath;
        path = path.substring(0, Math.max(0, path.length() - toTrim.length())); // Trim /json.html
        JsonResults[] jrs = semiMeterDao.performParameterizedQuery(startAt, endAt, path);
        return jrs;
    }

    @RequestMapping("/**")
    public String entry( HttpServletRequest req, Model model, @RequestParam String resolution ) {
        // PathInfo is the string behind "show", so "show/x" is "/x"
        String path = req.getPathInfo();
        if ( path == null ) {
            path = "";
        }
        // Sanity checking some parameters. Should not really matter. Notice that % is allowed
        if ( path.indexOf("'") != -1 || path.indexOf("`") != -1 || path.indexOf("|") != -1 ||
                path.indexOf(";") != -1 || path.indexOf("\\") != -1 || path.indexOf("&") != -1 ||
                path.indexOf("(") != -1 || path.indexOf(")") != -1 || path.indexOf("$") != -1
                ) {
            log.error("Disallowed character found and no value will be returned. Path: "+path);
            return "showcount";

        }

        // It is slightly tricky to get spring to map separate general paths, so this must be done manually
        if ( "/change".equals( req.getServletPath()) ) {
            return displayChange( path, model, resolution );
        } else {
            // Default to show
            return displayCurrent( path, model, resolution );
        }
    }

    private String displayCurrent(String path, Model model, String resolution) {
        long endAt = System.currentTimeMillis() - DEFAULT_SKEW_IN_MS;
        long startAt = calculateStartTimeFromResolution(resolution, endAt);

        model.addAttribute("numberOfItems", semiMeterDao.sumItems( startAt, endAt, path+"%" ));
        return "showcount";
    }

    private String displayChange(String path, Model model, String resolution) {
        long endAt = System.currentTimeMillis() - DEFAULT_SKEW_IN_MS;
        long startAt = calculateStartTimeFromResolution(resolution, endAt);
        long previousPeriod = calculateStartTimeFromResolution(resolution, startAt);
        long result = semiMeterDao.sumItems(startAt, endAt, path + "%").longValue() - semiMeterDao.sumItems(previousPeriod, startAt, path + "%").longValue();

        model.addAttribute("numberOfItems", Long.valueOf( result ));

        return "showcount";
    }

    private long calculateStartTimeFromResolution(String resolution, long endAt) {
        long startAt;
        if ( resolution.equalsIgnoreCase("second")) {
            startAt = endAt - 1000;
        } else if ( resolution.equalsIgnoreCase("minute")) {
            startAt = endAt - 60000;
        } else if ( resolution.equalsIgnoreCase("hour")) {
            startAt = endAt - 60000*60;
        } else if ( resolution.equalsIgnoreCase("day")) {
            startAt = endAt - 60000*60*24;
        } else if ( resolution.equalsIgnoreCase("week")) {
            startAt = endAt - 60000*60*24*7;
        } else if ( resolution.equalsIgnoreCase("month")) {
            // Using 30 day month
            startAt = endAt - 60000*60*24*7*30l;
        } else if ( resolution.equalsIgnoreCase("total")) {
            // Defaulting to total - beginning at time 0
            startAt = 0;
        } else {
            throw new RuntimeException("Did not understand resolution "+resolution);
        }
        return startAt;
    }
}
