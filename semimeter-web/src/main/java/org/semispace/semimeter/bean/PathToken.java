package org.semispace.semimeter.bean;

public class PathToken {
    private String tokenAlias;
    private boolean groupByThisToken;
    private String value;

    public PathToken(String value, String alias, boolean groupByThisToken) {
        this.value = value;
        this.tokenAlias = alias;
        this.groupByThisToken = groupByThisToken;
    }

    public String getTokenAlias() {
        return tokenAlias;
    }


    public boolean isGroupByThisToken() {
        return groupByThisToken;
    }


    public String getValue() {
        return value;
    }

}
