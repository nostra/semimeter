package org.semispace.semimeter.dao.util;

import org.junit.Test;
import org.semispace.semimeter.bean.PathToken;
import org.semispace.semimeter.bean.TokenizedPathInfo;
import org.semispace.semimeter.dao.helper.QueryTokenConverter;

import static junit.framework.Assert.*;

public class QueryTokenConverterTest {

    @Test
    public void test_empty_input() throws Exception {
        TokenizedPathInfo input = null;
        QueryTokenConverter result = new QueryTokenConverter(input);
        assertNotNull(result);
        assertNull(result.getQueryString());
        assertNull(result.getQueryAlias());

        input = new TokenizedPathInfo("/");
        result = new QueryTokenConverter(input);
        assertNotNull(result);
        assertNull(result.getQueryString());
        assertNull(result.getQueryAlias());
    }

    @Test
    public void test_one_token() throws Exception {
        TokenizedPathInfo input = new TokenizedPathInfo("/");
        input.addPathToken(new PathToken("95", "publicationID", false));
        QueryTokenConverter result = new QueryTokenConverter(input);
        assertNotNull(result);
        assertNull(result.getQueryString());
        assertNull(result.getQueryAlias());

        input = new TokenizedPathInfo("/");
        input.addPathToken(new PathToken(null, "publicationID", true));
        result = new QueryTokenConverter(input);
        assertNotNull(result);
        assertEquals("SUBSTRING_INDEX(path, '/', -1) as publicationID", result.getQueryString());
        assertEquals("publicationID", result.getQueryAlias());
    }

    @Test
    public void test_multiple_tokens_pass() throws Exception {
        TokenizedPathInfo input = new TokenizedPathInfo("|");
        input.addPathToken(new PathToken(null, "articleType", false));
        input.addPathToken(new PathToken("95", "publicationID", false));
        input.addPathToken(new PathToken(null, "sectionId", false));
        input.addPathToken(new PathToken(null, "articleId", true));
        QueryTokenConverter result = new QueryTokenConverter(input);
        assertNotNull(result);
        assertEquals("SUBSTRING_INDEX(path, '|', -1) as articleId", result.getQueryString());
        assertEquals("articleId", result.getQueryAlias());
    }

    @Test
    public void test_multiple_tokens_fail() throws Exception {
        TokenizedPathInfo input = new TokenizedPathInfo("|");
        input.addPathToken(new PathToken(null, "articleType", false));
        input.addPathToken(new PathToken("95", "publicationID", false));
        input.addPathToken(new PathToken(null, "sectionId", false));
        input.addPathToken(new PathToken(null, "articleId", false));
        QueryTokenConverter result = null;
        try {
            result = new QueryTokenConverter(input);
        } catch (IllegalArgumentException e) {
            //good, just what we expect
            assertNotNull(result);
        }
    }
}
