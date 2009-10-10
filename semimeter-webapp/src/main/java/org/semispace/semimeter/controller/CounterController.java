package org.semispace.semimeter.controller;

import org.semispace.semimeter.dao.SemiMeterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CounterController {
    private final Logger log = LoggerFactory.getLogger(CounterController.class);

    @Autowired
    private SemiMeterDao semiMeterDao;

    @RequestMapping("/**")
    public String showFeed( HttpServletRequest req, Model model ) {
        // PathInfo is the string behind "showfeed", so "showfeed/x" is "/x"
        String path = req.getPathInfo();
        if ( path == null ) {
            path = "";
        }

        // TODO QUERY DAO
        log.error("NOT IMPLEMENTED!!");
        model.addAttribute("numberOfElements", -100);
        return "textlist";
    }

    @RequestMapping("/index.html")
    public String entryPage() {
        return "index";
    }
}
