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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GroupedResult");
        sb.append("{keyName='").append(keyName).append('\'');
        sb.append(", key='").append(key).append('\'');
        sb.append(", count=").append(count);
        sb.append('}');
        return sb.toString();
    }
}
