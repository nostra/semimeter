package org.semispace.semimeter.bean;

import java.util.HashMap;
import java.util.Map;

public class GroupedResult {
    private String keyName;
    private String key;
    private Integer count = 0;
    private Map<String, Integer> trend = new HashMap<String, Integer>();

    private Map<String, Integer> splitCounts = new HashMap<String, Integer>();

    private String publicationId;

    public String getKeyName() {
        return keyName;
    }

    public String getKey() {
        return key;
    }

    public int getCount() {
        return count;
    }

    public Map<String, Integer> getTrend() {
        return trend;
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

    public Map<String, Integer> getSplitCounts() {
        return splitCounts;
    }

    public void setTrend(Map<String, Integer> trend) {
        this.trend = trend;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GroupedResult");
        sb.append("{keyName='").append(keyName).append('\'');
        sb.append(", key='").append(key).append('\'');
        sb.append(", publicationId=").append(publicationId);
        sb.append(", count=").append(count);
        sb.append(", splitCounts=").append(splitCounts);
        sb.append(", trend=").append(trend);
        sb.append('}');
        return sb.toString();
    }

    /**
     * @deprecated Use the version that takes a string, please
     */
    public void setPublicationId(Integer publicationId) {
        setPublicationId(""+publicationId);
    }
    public void setPublicationId(String publicationId) {
        this.publicationId = publicationId;
    }

    public String getPublicationId() {
        return this.publicationId;
    }
}
