package org.semispace.semimeter.bean;

public class DisplayResult {
    private final String path;
    private String str;

    public DisplayResult(String path) {
        this.path = path;
    }


    public String getPath() {
        return path;
    }

    public void setResult(String str) {
        this.str = str;
    }

    public String getResult() {
        return str;
    }
}
