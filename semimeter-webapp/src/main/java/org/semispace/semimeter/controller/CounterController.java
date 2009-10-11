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

import org.semispace.semimeter.dao.SemiMeterDao;
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

    @RequestMapping("/**")
    public String entry( HttpServletRequest req, Model model, @RequestParam String resolution ) {
        // PathInfo is the string behind "show", so "show/x" is "/x"
        String path = req.getPathInfo();
        if ( path == null ) {
            path = "";
        }

        long endAt = System.currentTimeMillis() - DEFAULT_SKEW_IN_MS;
        long startAt = endAt;
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
        } else {
            // Defaulting to total - beginning at time 0
            startAt = 0;
        }

        model.addAttribute("numberOfItems", semiMeterDao.sumItems( startAt, endAt, path+"%" ));
        return "showcount";
    }

    @RequestMapping("/index.html")
    public String entryPage() {
        return "index";
    }
}
