package org.semispace.semimeter.bean;

import java.util.List;

public class GroupedSumsResult {

    private String key;
    private List<GroupedResult> payload;

    public GroupedSumsResult() {
        //default constructor
    }

    public GroupedSumsResult(final String key, final List<GroupedResult> resultList) {
        this.key = key;
        this.payload = resultList;
    }

    public String getKey() {
        return key;
    }

    public List<GroupedResult> getPayload() {
        return payload;
    }

}
