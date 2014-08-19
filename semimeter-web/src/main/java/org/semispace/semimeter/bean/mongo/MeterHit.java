package org.semispace.semimeter.bean.mongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeterHit {
    private static final Logger log = LoggerFactory.getLogger(MeterHit.class);

    private Long when;
    private PathElements pathElements;
    private Integer count;

    public MeterHit() {
        //default
    }

    public MeterHit(Long when, String path, String delim, Integer count) {
        this.when = when;
        this.count = count;
        this.pathElements = calcPath(path, delim);
    }

    public static PathElements calcPath(final String path, final String delim) {
        PathElements result = null;
        if (path != null && delim != null) {
            String trimmedPath = path.trim();
            if (trimmedPath.startsWith(delim)) {
                trimmedPath = trimmedPath.substring(1);
            }
            String[] tokens = trimmedPath.split(delim);
            if (tokens != null && tokens.length > 0) {
                result = new PathElements();
                for (int i = 0; i < tokens.length; i++) {
                    String s = tokens[i];
                    if (s != null && !s.isEmpty() && !"%".equals(s) && !"*".equals(s) && ! "_".equals(s)) {
                        try {
                            result.getClass().getMethod("setE" + (i + 1), String.class).invoke(result, s);
                        } catch (Exception e) {
                            log.error("Expected never to happen", e);
                        }
                    }
                }
            }
        }

        return result;
    }

    public Long getWhen() {
        return when;
    }

    public void setWhen(final Long when) {
        this.when = when;
    }


    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }


    public PathElements getPathElements() {
        return pathElements;
    }

    public void setPathElements(final PathElements pathElements) {
        this.pathElements = pathElements;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MeterHit");
        sb.append("{when=").append(when);
        sb.append(", pathElements=").append(pathElements);
        sb.append(", count=").append(count);
        sb.append('}');
        return sb.toString();
    }
}
