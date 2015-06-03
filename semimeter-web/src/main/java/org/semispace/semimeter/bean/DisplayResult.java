package org.semispace.semimeter.bean;

public class DisplayResult {
    private final String key;
    private Object result;

    public DisplayResult(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
}
