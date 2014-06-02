/******************************************************************
 * File:        TestSearch.java
 * Created by:  Dave Reynolds
 * Created on:  25 Feb 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.util.PrefixUtils;
import com.hp.hpl.jena.query.ResultSetFactory;

/**
 * Testing multi-predicate text search handling.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestSearch {
    App testapp;
    SparqlSource src;
    
    @Before
    public void startup() throws IOException {
        testapp = new App("testapp", new File("src/test/textSearch/app.conf"));
        src = testapp.getComponentAs("ssource", SparqlSource.class);
    }
    
    @Test
    public void testAll() {
        assertEquals(5, numMatches("'Somerset'"));
        
        // These options would require per-predicate text index
//        assertEquals(2, numMatches("(rdfs:label 'Somerset')"));
//        assertEquals(3, numMatches("(eg:label   'Somerset')"));
    }

    private int numMatches(String textQuery) {
        String query = "SELECT ?item WHERE {?item text:query " + textQuery + " . }";
        query = PrefixUtils.expandQuery(query, testapp.getPrefixes());
        return ResultSetFactory.makeRewindable( src.select(query) ).size();
    }
    
}
