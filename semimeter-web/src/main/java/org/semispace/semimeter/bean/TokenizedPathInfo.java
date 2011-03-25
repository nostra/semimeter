package org.semispace.semimeter.bean;

import java.util.ArrayList;
import java.util.Collections;
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
        return Collections.unmodifiableList(pathTokens);
    }


    public void addPathToken(PathToken pathToken) {
        this.pathTokens.add(pathToken);
    }

    public String buildPathFromTokens() {
        StringBuilder sb = new StringBuilder();

        for (PathToken token : pathTokens) {
            sb.append(pathTokenDelimeter);
            sb.append(pathify(token.getValue()));
        }
        return sb.toString();
    }


    private String pathify(String value) {
        if (value == null || value.isEmpty()) {
            return "%";
        }
        return value;
    }
}
