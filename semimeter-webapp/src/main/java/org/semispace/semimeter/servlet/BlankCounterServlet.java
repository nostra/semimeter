package org.semispace.semimeter.servlet;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.semimeter.space.ZeroAbleBlankCounter;
import org.semispace.semimeter.space.EnsuringResetDuringIdle;
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
    private SemiEventRegistration registration;

    public void init() {
        log.debug("Initializing");
        blankImage = readBlankImage();
        space = SemiSpace.retrieveSpace();
        counter = new ZeroAbleBlankCounter(space);
        registration = space.notify(new EnsuringResetDuringIdle(), this, SemiSpace.ONE_DAY*3650);
    }


    public void destroy() {
        log.debug("Shutting down");
        counter.reset();
        registration.getLease().cancel();
    }

    /**
     * If pathInfo ends with .gif, it is trimmed. Otherwise, the path is retained in its enterity
     * @param request
     * @param response
     * @throws IOException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // I know that semimeter/c/xx/yy.gif will resolve to /xx/yy.gif with PathInfo
        String path = request.getPathInfo();
        if ( path.endsWith(".gif")) {
            path = path.substring(0, path.length() - 4);
        }
        long now = System.currentTimeMillis();
        // Math abs is in order to handle clock skew
        if ( Math.abs(now - lastReset) > 1000 ) {
            lastReset = now;
            EnsuringResetDuringIdle erdi = new EnsuringResetDuringIdle();
            space.takeIfExists(erdi);
            space.write(erdi, 10000);
            counter.reset();
        }
        counter.count(path);
        response.setContentType("image/gif");
        response.getOutputStream().write(blankImage);
    }

    private byte[] readBlankImage() {
        InputStream is = getClass().getResourceAsStream("/image/blank.gif");
        byte[] bytes = new byte[128]; // I know the image has a size of 64B
        // Read in the bytes
        int offset = 0;
        try {
            int numRead = 0;
            while (offset < bytes.length
                   && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
        } catch (IOException e) {
            log.error("Totally unexpected. Was not able to read image", e);
        }
        byte[] result = Arrays.copyOf(bytes, offset);
        log.debug("Resulting length: "+result);
        return result;
    }

    @Override
    public void notify(SemiEvent theEvent) {
        if ( theEvent instanceof SemiExpirationEvent) {
            // Notice that the expiration event will not be called the moment it occurs
            log.debug("Calling reset because of expiration of idle element.");
            counter.reset();
        }
    }
}
