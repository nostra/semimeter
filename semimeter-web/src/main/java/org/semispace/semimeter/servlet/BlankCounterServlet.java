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

package org.semispace.semimeter.servlet;

import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.semimeter.space.CounterHolder;
import org.semispace.semimeter.space.EnsuringResetDuringIdle;
import org.semispace.semimeter.space.ZeroAbleBlankCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Serve blank gif and also take care of all SemiSpace related initializations.
 */
public class BlankCounterServlet extends HttpServlet implements SemiEventListener {
    private static Logger log = LoggerFactory.getLogger(BlankCounterServlet.class);

    private SemiSpaceInterface space;
    private byte[] blankImage;
    private long lastReset = System.currentTimeMillis();
    private ZeroAbleBlankCounter counter;
    private SemiEventRegistration purgehelperWhenIdle;
    private long resolution_ms;
    private int throttle;

    public void init() {
        log.debug("Initializing");
        blankImage = readBlankImage();
        space = SemiSpace.retrieveSpace();
        counter = new ZeroAbleBlankCounter(space);
        purgehelperWhenIdle = space.notify(new EnsuringResetDuringIdle(), this, SemiSpace.ONE_DAY * 3650);
        throttle = 0;
        String res = System.getProperty(CounterHolder.RESOLUTION_MS_SYSTEM_VARIABLE);
        if (res != null) {
            resolution_ms = Long.valueOf(res);
        } else {
            resolution_ms = 1000;
            log.error("Missing system property " + CounterHolder.RESOLUTION_MS_SYSTEM_VARIABLE +
                    " please add it to the java vm as a -D parameter. Using default of " + resolution_ms);
        }
        log.info("Using {} as least update interval.", resolution_ms);
    }


    public void destroy() {
        log.debug("Shutting down");
        counter.reset();
        purgehelperWhenIdle.getLease().cancel();
    }

    /**
     * If pathInfo ends with .gif, it is trimmed. Otherwise, the path is retained in its enterity.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // I know that semimeter/c/xx/yy.gif will resolve to /xx/yy.gif with PathInfo
        String path = request.getPathInfo();
        recordAndReturnBlankImage(response, path);
    }


    /**
     * Method which is extension friendly. Remember to do super.recordAndReturnBlankImage( ... ) after override.
     * @param pathInfo Result from request.getPathInfo();
     */
    public void recordAndReturnBlankImage(HttpServletResponse response, String pathInfo) throws IOException {
        String path = pathInfo;
        if (path.endsWith(".gif")) {
            path = path.substring(0, path.length() - 4);
        }
        long now = System.currentTimeMillis();
        // Math abs is in order to handle clock skew
        long throttledRes = (resolution_ms * (throttle + 1));
        if (Math.abs(now - lastReset) > throttledRes) {
            lastReset = now;
            EnsuringResetDuringIdle erdi = new EnsuringResetDuringIdle();
            space.takeIfExists(erdi);
            space.write(erdi, 10000 * (throttle + 1));
            counter.reset();
        }
        counter.count(path);
        // Trying to avoid any kind of browser or site caching, add no cache headers as mad man.
        // Adapted from:
        // http://stackoverflow.com/questions/511144/how-to-instruct-web-browsers-not-to-cache-pages
        response.addHeader("Cache-Control", "no-cache"); //HTTP 1.1
        response.addHeader("Cache-Control", "must-revalidate"); // HTTP 1.1
        response.addHeader("Pragma", "no-cache"); // HTTP 1.0
        response.addHeader("Expires", "Wed, 09 Jun 1999 00:00:00 GMT"); // HTTP 1.0
        /* The following headers are slightly exotic, therefore commented out. */
        //response.addHeader("Cache-Control", "private"); // HTTP 1.1
        //response.addHeader("Cache-Control", "no-store"); // HTTP 1.1
        //response.addHeader("Cache-Control", "max-stale=0"); // HTTP 1.1
        //response.addHeader("Cache-Control", "post-check=0"); // HTTP 1.1
        //response.addHeader("Cache-Control", "pre-check=0"); // HTTP 1.1

        response.setContentType("image/gif");
        response.getOutputStream().write(blankImage);
    }


    private byte[] readBlankImage() {
        try (InputStream is = getClass().getResourceAsStream("/image/blank.gif")) {

            byte[] bytes = new byte[128]; // I know the image has a size of 79B
            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            byte[] result = Arrays.copyOf(bytes, offset);
            log.debug("Resulting length: " + result);
            return result;
        } catch (IOException e) {
            log.error("Got exception.", e);
            return new byte[0];
        }
    }

    @Override
    public void notify(SemiEvent theEvent) {
        if (theEvent instanceof SemiExpirationEvent) {
            // Notice that the expiration event will not be called the moment it occurs
            log.debug("Calling reset because of expiration of idle element.");
            counter.reset();
        }
    }
}
