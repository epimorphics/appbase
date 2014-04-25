/******************************************************************
 * File:        TestRemoteSource.java
 * Created by:  Dave Reynolds
 * Created on:  24 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.impl.RemoteSparqlSource;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

public class TestRemoteSource {
    protected static final String TEST = "http://localhost/test/def#";
    
    App app;
    SparqlSource source;
    
    /**
     * Override to change the source to be tested.
     * Assumes there is a test fuseki on 4040
     * <pre>
     *   fuseki-server --update --mem --port=4040 /test
     * </pre>
     */
    // Not an @Before because we don't want it to run if the test is suppressed
    protected void setup() {
        app = new App("remote source test");
        RemoteSparqlSource source = new RemoteSparqlSource();
        source.setEndpoint("http://localhost:4040/test/query");
        source.setUpdateEndpoint("http://localhost:4040/test/update");
        source.setGraphEndpoint("http://localhost:4040/test/data");
        app.addComponent("source", source);
        app.startup();
        
        this.source = source;
    }
    
    // This test is normally excluded
    @Ignore @Test
    public void testRemoveSource() {
        setup();
        
        assertTrue(source.isUpdateable());
        
        DatasetAccessor accessor = source.getAccessor();
        for (int i = 0; i < 2; i++) {
            Model m = ModelFactory.createDefaultModel();
            m.createResource(TEST + "i" + i)
                .addProperty(RDFS.label, "In graph " + i);
            accessor.putModel(TEST + "g" + i, m);
        }
        
        checkLabels(new String[]{"In graph 0", "In graph 1"});

        String update = "" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "WITH <http://localhost/test/def#g1> \n" +
                "DELETE {?x rdfs:label ?l} \n" +
                "INSERT {?x rdfs:label 'new string'} \n" +
                "WHERE {?x rdfs:label ?l}";
        source.update( UpdateFactory.create(update) );

        checkLabels(new String[]{"In graph 0", "new string"});

        // clean up
        for (int i = 0; i < 2; i++) {
            accessor.deleteModel(TEST + "g" + i);
        }
        app.shutdown();
    }
    
    private void checkLabels(String[] labels) {
        ResultSet rs = source.select("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT * WHERE { GRAPH ?g { ?i rdfs:label ?label } } ORDER BY ?g");

        for (int i = 0; i < labels.length; i++) {
            assertTrue( rs.hasNext() );
            QuerySolution row = rs.next();
            assertEquals( "g" + i, row.getResource("g").getLocalName());
            assertEquals( labels[i], row.getLiteral("label").getLexicalForm() );
        }
    }
}
