package com.yagay.NotifyLens.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchQueryTest {
    @Test
    public void buildsPrefixAndQuery() {
        assertEquals("\"hello\"* AND \"world\"*", SearchQuery.fts("hello world"));
    }

    @Test
    public void stripsFtsSyntaxCharacters() {
        assertEquals("\"hello\"*", SearchQuery.fts("\"hello\"*"));
    }
}
