package org.semispace.semimeter.bean;

public class GroupedResult {
    private String keyName;
    private String key;
    private int count;

    public String getKeyName() {
        return keyName;
    }

    public String getKey() {
        return key;
    }

    public int getCount() {
        return count;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
