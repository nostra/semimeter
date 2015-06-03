package org.semispace.semimeter.space;


import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.semimeter.bean.ArrayQuery;
import org.semispace.semimeter.bean.GroupedSumsQuery;
import org.semispace.semimeter.bean.ParameterizedQuery;
import org.semispace.semimeter.bean.TruncateTimeout;
import org.semispace.semimeter.dao.SemiMeterDao;
import org.semispace.semimeter.space.listener.Space2Dao;
import org.semispace.semimeter.space.listener.SpacePQListener;
import org.semispace.semimeter.space.listener.TruncateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class SpaceRegistration {

    private static final Logger log = LoggerFactory.getLogger(SpaceRegistration.class);
    private SemiSpaceInterface space;

    private SemiEventRegistration chRegistration;
    private SemiEventRegistration pqRegistration;
    private SemiEventRegistration aqRegistration;
    private SemiEventRegistration groupedSumsRegistration;
    private SemiEventRegistration truncateRegistration;

    @Autowired
    private SemiMeterDao semimeterDao;

    @PostConstruct
    public void initialize() {
        log.debug("Retrieving semispace.");
        space = SemiSpace.retrieveSpace();
        log.debug("Registering listeners.");
        if (chRegistration != null || pqRegistration != null || aqRegistration != null ||
                groupedSumsRegistration != null || truncateRegistration != null) {
            log.error("Did not expect any SemiSpace registration to exist already. Not registering again");
        } else {
            SpacePQListener spacePqListener =
                    new SpacePQListener(space, semimeterDao, "Query listener - both parameterized and array queries");
            // Listen for events ten years
            chRegistration = space.notify(new CounterHolder(),
                    new Space2Dao(space, semimeterDao, "CounterHolder which holds elements to be counted"),
                    SemiSpace.ONE_DAY * 3650);
            // Reusing
            pqRegistration = space.notify(new ParameterizedQuery(), spacePqListener, SemiSpace.ONE_DAY * 3650);
            aqRegistration = space.notify(new ArrayQuery(), spacePqListener, SemiSpace.ONE_DAY * 3650);
            groupedSumsRegistration = space.notify(new GroupedSumsQuery(), spacePqListener, SemiSpace.ONE_DAY * 3650);

            TruncateListener truncateListener = new TruncateListener(space, semimeterDao,
                    "Truncate Listner, truncates data whenever TruncateTimeout bean expires");
            truncateRegistration = space.notify(new TruncateTimeout(), truncateListener, SemiSpace.ONE_DAY * 3650);
            truncateListener.triggerTimeout();
        }

    }

    @PreDestroy
    public void destroy() {
        if (chRegistration != null) {
            chRegistration.getLease().cancel();
            chRegistration = null;
        }
        if (pqRegistration != null) {
            pqRegistration.getLease().cancel();
            pqRegistration = null;
        }
        if (aqRegistration != null) {
            aqRegistration.getLease().cancel();
            aqRegistration = null;
        }
        if (groupedSumsRegistration != null) {
            groupedSumsRegistration.getLease().cancel();
            groupedSumsRegistration = null;
        }
        if (truncateRegistration != null) {
            truncateRegistration.getLease().cancel();
            truncateRegistration = null;
        }
    }
}
