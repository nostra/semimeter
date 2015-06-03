package org.semispace.semimeter.bean;

import java.util.ArrayList;
import java.util.List;

public class TokenizedPathInfo {
    private final String pathTokenDelimeter;
    private final List<PathToken> pathTokens = new ArrayList<PathToken>();

    public TokenizedPathInfo(final String pathTokenDelimiter) {
        this.pathTokenDelimeter = pathTokenDelimiter;
    }

    public String getPathTokenDelimeter() {
        return pathTokenDelimeter;
    }

    public List<PathToken> getPathTokens() {
        //return Collections.unmodifiableList(pathTokens);
        return pathTokens;
    }


    public void addPathToken(PathToken pathToken) {
        this.pathTokens.add(pathToken);
    }

    public String buildPathFromTokens() {
        StringBuilder sb = new StringBuilder();

        for (PathToken token : pathTokens) {
            sb.append(pathTokenDelimeter);
            sb.append(pathify(token));
        }
        return sb.toString();
    }


    private String pathify(PathToken token) {
        if (token.getValue() == null || token.getValue().isEmpty() || token.isGroupByThisToken()) {
            return "%";
        }
        return token.getValue();
    }
}
