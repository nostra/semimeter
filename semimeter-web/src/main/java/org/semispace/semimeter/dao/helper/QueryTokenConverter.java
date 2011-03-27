package org.semispace.semimeter.dao.helper;

import org.semispace.semimeter.bean.PathToken;
import org.semispace.semimeter.bean.TokenizedPathInfo;

import java.util.ArrayList;
import java.util.List;

public class QueryTokenConverter {

    private TokenizedPathInfo tokenizedPathInfo;
    private String queryString;
    private String queryAlias;

    public QueryTokenConverter(final TokenizedPathInfo input) throws IllegalArgumentException {
        this.tokenizedPathInfo = input;
        List<String> result = new ArrayList<String>();
        if (input != null) {
            for (PathToken token : input.getPathTokens()) {
                if (token.isGroupByThisToken()) {
                    if (token == input.getPathTokens().get(input.getPathTokens().size() - 1)) {
                        queryString = "SUBSTRING_INDEX(path, '" + input.getPathTokenDelimeter() + "', -1) as " + token.getTokenAlias();
                        queryAlias = token.getTokenAlias();
                    } else {
                        throw new IllegalArgumentException("TokenizedPathInfo only supports one group token which must be at the end of the path");
                    }
                }
            }
        }
    }

    public String getQueryString() {
        return queryString;
    }

    public String getQueryAlias() {
        return queryAlias;
    }
}
