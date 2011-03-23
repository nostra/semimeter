package org.semispace.semimeter.bean;

public class DisplayResult {
    private final String path;
    private Long count;

    public DisplayResult(String path) {
        this.path = path;
    }


    public String getPath() {
        return path;
    }

    public void setResult(Long count) {
        this.count = count;
    }

    public Long getResult() {
        return count;
    }
}
