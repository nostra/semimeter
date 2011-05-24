package org.semispace.semimeter.bean.mongo;

import java.util.ArrayList;
import java.util.List;

public class MeterHit {
    private Long when;
    private List<String> pathElements = new ArrayList<String>();
    private Integer count;

    public MeterHit() {
        //default
    }

    public MeterHit(Long when, String path, String delim, Integer count) {
        this.when = when;
        this.count = count;
        if (path != null && delim != null) {
            for (String token : path.split(delim)) {
                this.pathElements.add(token);
            }
        }
    }

    public Long getWhen() {
        return when;
    }

    public void setWhen(final Long when) {
        this.when = when;
    }

    public List<String> getPathElements() {
        return pathElements;
    }

    public void setPathElements(final List<String> pathElements) {
        this.pathElements = pathElements;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}
