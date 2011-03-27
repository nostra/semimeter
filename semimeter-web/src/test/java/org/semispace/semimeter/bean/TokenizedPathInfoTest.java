package org.semispace.semimeter.bean;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class TokenizedPathInfoTest {
    @Test
    public void testGetPathTokenDelimeter() throws Exception {
        TokenizedPathInfo test = new TokenizedPathInfo("DELIM");
        assertEquals("DELIM", test.getPathTokenDelimeter());
    }

    @Test
    public void testGetPathTokens() throws Exception {
        TokenizedPathInfo test = new TokenizedPathInfo("DELIM");
        test.addPathToken(new PathToken("val", "alias", false));
        assertEquals(1, test.getPathTokens().size());

    }

    @Test
    public void testBuildPathFromTokens_1() throws Exception {
        TokenizedPathInfo test = new TokenizedPathInfo("/");
        test.addPathToken(new PathToken("val", "alias", false));
        assertEquals("/val", test.buildPathFromTokens());
    }

    @Test
    public void testBuildPathFromTokens_2() throws Exception {
        TokenizedPathInfo test = new TokenizedPathInfo("/");
        test.addPathToken(new PathToken(null, "alias", false));
        assertEquals("/%", test.buildPathFromTokens());
    }

    @Test
    public void testBuildPathFromTokens_3() throws Exception {
        TokenizedPathInfo test = new TokenizedPathInfo("/");
        test.addPathToken(new PathToken("", "alias", false));
        assertEquals("/%", test.buildPathFromTokens());
    }

    @Test
    public void testBuildPathFromTokens_4() throws Exception {
        TokenizedPathInfo test = new TokenizedPathInfo("/");
        test.addPathToken(new PathToken("val", "alias", true));
        assertEquals("/%", test.buildPathFromTokens());
    }

    @Test
    public void testBuildPathFromTokens_5() throws Exception {
        TokenizedPathInfo test = new TokenizedPathInfo("/");
        test.addPathToken(new PathToken("val1", "alias1", false));
        test.addPathToken(new PathToken("val2", "alias2", false));
        test.addPathToken(new PathToken("val3", "alias3", false));
        test.addPathToken(new PathToken("val4", "alias4", true));
        assertEquals("/val1/val2/val3/%", test.buildPathFromTokens());
    }

    @Test
    public void testBuildPathFromTokens_6() throws Exception {
        TokenizedPathInfo test = new TokenizedPathInfo("/");
        test.addPathToken(new PathToken(null, "articleType", false));
        test.addPathToken(new PathToken("95", "publication", false));
        test.addPathToken(new PathToken(null, "section", false));
        test.addPathToken(new PathToken(null, "article", true));
        assertEquals("/%/95/%/%", test.buildPathFromTokens());
    }
}
