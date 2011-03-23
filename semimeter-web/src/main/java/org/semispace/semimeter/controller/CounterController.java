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

import org.semispace.semimeter.bean.JsonResults;
import org.semispace.semimeter.service.JsonService;
import org.semispace.semimeter.service.SemimeterService;
import org.semispace.semimeter.space.CounterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class CounterController {
    private final Logger log = LoggerFactory.getLogger(CounterController.class);

    @Autowired
    private SemimeterService semimeterService;

    @Autowired
    private JsonService jsonService;

    @RequestMapping("/index.html")
    public String entryPage() {
        return "index";
    }


    /**
     * Queries on graph strongly correlates to queries on json.html, URI-wise
     */
    @RequestMapping("/**/graph.html")
    public String graphPage(Model model, HttpServletRequest request, @RequestParam String resolution) {
        if (!isSane(request.getServletPath())) {
            throw new RuntimeException("Disallowed character found in query.");
        }
        long endAt = semimeterService.getCurrentEndTime();
        long startAt = semimeterService.calculateStartTimeFromResolution(resolution, endAt);
        JsonResults[] jrs = semimeterService.getJsonResults(trimPath("/graph.html", request.getServletPath()), endAt,
                startAt, resolution);
        long max = 0;
        for (JsonResults jr : jrs) {
            max = Math.max(max, Long.valueOf(jr.getValue()).longValue());
        }
        max++;
        max *= 1.25;

        model.addAttribute("resolution", resolution);
        model.addAttribute("xAxisSize", Long.valueOf(max));

        long updtFreq;
        if ("second".equalsIgnoreCase(resolution)) {
            updtFreq = 2000;
        } else if ("minute".equalsIgnoreCase(resolution)) {
            updtFreq = 30000;
        } else {
            // Default to every minute
            updtFreq = 60000;
        }
        String res = System.getProperty(CounterHolder.RESOLUTION_MS_SYSTEM_VARIABLE);
        if (res != null) {
            updtFreq = Math.max(updtFreq, 2 * Long.valueOf(res));
        }

        model.addAttribute("updateInterval", updtFreq);

        return "bargraph";
    }

    @RequestMapping("/**/monitor.html")
    public String monitorPage(Model model, HttpServletRequest request, @RequestParam(required = false) String graphresolution, @RequestParam(required = false) String counterresolution) {
        if (!isSane(request.getServletPath())) {
            throw new RuntimeException("Disallowed character found in query.");
        }
        if (graphresolution == null) {
            graphresolution = "month";
        }
        if (counterresolution == null) {
            counterresolution = "total";
        }
        semimeterService.calculateNumberOfSamples(counterresolution); // Just in order to get an exception if value is wrong
        model.addAttribute("graphresolution", graphresolution);
        model.addAttribute("counterresolution", counterresolution);
        model.addAttribute("path", trimPath("/monitor.html", request.getServletPath()));
        model.addAttribute("graphsamples", semimeterService.calculateNumberOfSamples(graphresolution));

        return "monitor";
    }

    @RequestMapping("/**/json.html")
    public String showData(Model model, HttpServletRequest request, @RequestParam String resolution) {
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
        if (!isSane(request.getServletPath())) {
            throw new RuntimeException("Disallowed character found in query.");
        }

        long endAt = semimeterService.getCurrentEndTime();
        long startAt = semimeterService.calculateStartTimeFromResolution(resolution, endAt);

        JsonResults[] jrs = semimeterService.getJsonResults(trimPath("/json.html", request.getServletPath()), endAt, startAt, resolution);
        String str = jsonService.createJsonStringFromArray(jrs);

        model.addAttribute("numberOfItems", str);

        return "showcount";
    }

    /**
     *
     */
    @RequestMapping("/**/array.html")
    public String showArray(Model model, HttpServletRequest request, HttpServletResponse response, @RequestParam String resolution, @RequestParam Integer numberOfSamples) {
        if (numberOfSamples.intValue() < 1) {
            throw new RuntimeException("numberOfSamples must be larger than 0.");
        }
        if (!isSane(request.getServletPath())) {
            throw new RuntimeException("Disallowed character found in query.");
        }
        String path = trimPath("/array.html", request.getServletPath());
        long endAt = semimeterService.getCurrentEndTime();
        long startAt = semimeterService.calculateStartTimeFromResolution(resolution, endAt);
        JsonResults[] jrs = semimeterService.getArrayCounts(resolution, numberOfSamples, path, endAt, startAt);

        String str = jsonService.createJsonStringFromArray(jrs);
        model.addAttribute("numberOfItems", str);

        return "showcount";
    }

    /**
     * Really a mapping of /show/ and /change/
     */
    @RequestMapping("/**")
    public String entry(HttpServletRequest req, Model model, @RequestParam String resolution) {
        // PathInfo is the string behind "show", so "show/x" is "/x"
        String path = req.getPathInfo();
        if (path == null) {
            path = "";
        }

        if (!isSane(path)) {
            throw new RuntimeException("Disallowed character found in query.");
        }

        // It is slightly tricky to get spring to map separate general paths, so this must be done manually
        if ("/change".equals(req.getServletPath())) {
            return displayChange(path, model, resolution);
        } else {
            // Default to show
            return displayCurrent(path, model, resolution);
        }
    }

    /**
     * Sanity checking some parameters. Should not really matter. Notice that % and $ is allowed
     * TODO Throw exception here instead
     */
    private boolean isSane(String path) {
        if (path.indexOf("'") != -1 || path.indexOf("`") != -1 || path.indexOf("|") != -1 ||
                path.indexOf(";") != -1 || path.indexOf("\\") != -1 || path.indexOf("&") != -1 ||
                path.indexOf("(") != -1 || path.indexOf(")") != -1) {
            log.error("Disallowed character found and false will be returned. Path: " + path);
            return false;
        }
        return true;
    }

    private String trimPath(String toTrim, String path) {
        return path.substring(0, Math.max(0, path.length() - toTrim.length())); // Trim /json.html - for instance
    }


    private String displayCurrent(String path, Model model, String resolution) {
        long endAt = semimeterService.getCurrentEndTime();
        long startAt = semimeterService.calculateStartTimeFromResolution(resolution, endAt);

        Long count = semimeterService.getCurrentCount(path, resolution, endAt, startAt);

        if (count != null) {
            JsonResults[] jrs = new JsonResults[1];
            jrs[0] = new JsonResults();
            jrs[0].setKey("show");
            jrs[0].setValue("" + count);
            model.addAttribute("numberOfItems", jsonService.createJsonStringFromArray(jrs));
        }

        return "showcount";
    }


    private String displayChange(String path, Model model, String resolution) {
        long endAt = semimeterService.getCurrentEndTime();
        long startAt = semimeterService.calculateStartTimeFromResolution(resolution, endAt);
        long previousPeriod = semimeterService.calculateStartTimeFromResolution(resolution, startAt);
        long result = semimeterService.getDeltaFromDb(path, endAt, startAt, previousPeriod);

        JsonResults[] jrs = new JsonResults[1];
        jrs[0] = new JsonResults();
        jrs[0].setKey("change");
        jrs[0].setValue("" + result);
        String str = jsonService.createJsonStringFromArray(jrs);

        model.addAttribute("numberOfItems", str);

        return "showcount";
    }

}
