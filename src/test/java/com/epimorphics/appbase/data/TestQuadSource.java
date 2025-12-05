/******************************************************************
 * File:        TestUnionSource.java
 * Created by:  Dave Reynolds
 * Created on:  25 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.impl.QuadFileSparqlSource;
import com.epimorphics.util.TestUtil;

public class TestQuadSource {
    protected static final String TEST = "http://localhost/test/def#";
    
    App app;
    QuadFileSparqlSource source;
    
    @BeforeEach
    public void setup() {
        app = new App("test");
        source = new QuadFileSparqlSource();
        source.setFile( "src/test/data/quadTest/data.nq" );
        source.setUnionDefault(true);
        app.addComponent("source", source);
        app.startup();
    }
    
    @Test
    public void testQuadSource() {
        // Check union
        TestUtil.testArray(checkGraphs(source), new String[]{"graph1", "graph2"});
        // Check graphs
        DatasetAccessor accessor = source.getAccessor();
        assertFalse(accessor.getModel(TEST + "graph1").isEmpty());
        assertFalse(accessor.getModel(TEST + "graph2").isEmpty());
        assertTrue(accessor.getModel(TEST + "graph3").isEmpty());
    }
    
    public static List<String> checkGraphs(SparqlSource source) {
        ResultSet rs = source.select("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?label WHERE {[] rdfs:label ?label}");
        List<String> results = new ArrayList<>();
        while (rs.hasNext()) {
            results.add( rs.next().getLiteral("label").getLexicalForm() );
        }
        return results;
    }

}
