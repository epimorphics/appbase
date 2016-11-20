/******************************************************************
 * File:        TestUnionSource.java
 * Created by:  Dave Reynolds
 * Created on:  25 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.impl.UnionDatasetSparqlSource;
import com.epimorphics.util.TestUtil;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDFS;

public class TestUnionSource {
    protected static final String TEST = "http://localhost/test/def#";
    
    App app;
    SparqlSource source;
    
    @Before
    public void setup() {
        app = new App("test");
        source = new UnionDatasetSparqlSource();
        app.addComponent("source", source);
        app.startup();
    }
    
    @Test
    public void testUnionSource() {
        DatasetAccessor accessor = source.getAccessor();
        accessor.putModel(TEST + "g1", createGraph("graph1"));
        accessor.putModel(TEST + "g2", createGraph("graph2"));
        TestUtil.testArray(checkGraphs(source), new String[]{"graph1", "graph2"});
        
        accessor.putModel(TEST + "g1", createGraph("graph1-b"));
        TestUtil.testArray(checkGraphs(source), new String[]{"graph1-b", "graph2"});
        
        accessor.deleteModel(TEST + "g2");
        TestUtil.testArray(checkGraphs(source), new String[]{"graph1-b"});
    }
    
    public static Model createGraph(String marker) {
        Model m = ModelFactory.createDefaultModel();
        m.createResource(TEST + "i")
            .addProperty(RDFS.label, marker);
        return m;
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
