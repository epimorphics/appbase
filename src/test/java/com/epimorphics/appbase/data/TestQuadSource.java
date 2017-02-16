/******************************************************************
 * File:        TestUnionSource.java
 * Created by:  Dave Reynolds
 * Created on:  25 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.ResultSet;
import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.impl.QuadFileSparqlSource;
import com.epimorphics.util.TestUtil;

public class TestQuadSource {
    protected static final String TEST = "http://localhost/test/def#";
    
    App app;
    QuadFileSparqlSource source;
    
    @Before
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
        assertTrue( accessor.containsModel(TEST + "graph1") );
        assertTrue( accessor.containsModel(TEST + "graph2") );
        assertFalse( accessor.containsModel(TEST + "graph3") );
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
