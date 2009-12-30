package org.semispace.semimeter.bean;

/**
 * Sent when the insertion queue is found to be too large, and
 * that the application itself needs to be slowed down.
 */
public class ThrottleBean {
    private Integer value;

    public ThrottleBean(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
