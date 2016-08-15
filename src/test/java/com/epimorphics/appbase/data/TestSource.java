/******************************************************************
 * File:        TestSource.java
 * Created by:  Dave Reynolds
 * Created on:  28 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.epimorphics.appbase.util.SQueryUtil;
import com.epimorphics.util.PrefixUtils;
import com.epimorphics.util.TestUtil;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.vocabulary.RDFS;

public class TestSource extends BaseSourceTest {

    @Override
    protected String getTestFileDir() {
        return "src/test/data/source-tests/base";
    }

    @Test
    public void testWrapper() {
        // Labels
        assertEquals("Pref label", getNode("test:i1").getLabel());
        assertEquals("Alt label",  getNode("test:i2").getLabel());
        assertEquals("rdfs label", getNode("test:i3").getLabel());
        assertEquals("name",       getNode("test:i4").getLabel());
        
        WNode node = getNode("test:i5");
        assertEquals("en label", node.getLabel("en"));
        assertEquals("plain label", node.getLabel("cy"));
        
        assertTrue(node.isResource());
        assertTrue(node.isURIResource());
        assertFalse(node.isLiteral());
        assertEquals("http://www.epimorphics.com/vocabs/test/i5", node.getURI());
        
        DatasetGraph dsg = source.constructViews("?uri skos:altLabel ?rdfs_label", TEST_NS + "i1", TEST_NS + "i2");
        checkLabel(dsg, "i1", "Alt label");
        checkLabel(dsg, "i2", "Alt label");
        
        // General accessors
        WNode test = getNode("test:test");
        WNode v = test.getPropertyValue("test:num");
        assertNotNull(v);
        assertTrue(v.isLiteral());
        assertTrue(v.isNumber());
        assertEquals(42, v.asInt());
        
        v = test.getPropertyValue("test:float");
        assertNotNull(v);
        assertTrue(v.isLiteral());
        assertTrue(v.isNumber());
        assertEquals(3.14, v.asFloat(), 0.001);
        
        v = test.getPropertyValue("test:string");
        assertNotNull(v);
        assertEquals("a string", v.getLabel());
        
        List<WNode> values = test.listPropertyValues("test:resource");
        assertEquals(2, values.size());
        TestUtil.testArray(values, new WNode[]{ getNode("test:i1"), getNode("test:i2")});
        
        // Lists
        WNode list = test.getPropertyValue("test:list");
        assertNotNull(list);
        assertTrue(list.isList());
        List<WNode> elts = list.asList();
        assertEquals(3,  elts.size());
        assertEquals(1, elts.get(0).asInt());
        assertEquals(3, elts.get(1).asInt());
        assertEquals(5, elts.get(2).asInt());
        
        // Connections
        checkConnections( getNode("test:c").listInLinks("test:p"), new String[]{"test:a", "test:b"} );
        checkConnections( getNode("test:d").listInLinks("test:p"), new String[]{"test:c"} );
        checkConnections( getNode("test:c").listInLinks("test:q"), new String[]{"test:f"} );

        checkConnections( getNode("test:c").connectedNodes("test:p / test:p"), new String[]{"test:e"} );
        
        List<PropertyValue> connections = getNode("test:c").listInLinks();
        assertEquals(2, connections.size());
        assertEquals(getNode("test:p"), connections.get(0).getProp());
        checkConnections( connections.get(0).getValues(), new String[]{"test:a", "test:b"});
        assertEquals(getNode("test:q"), connections.get(1).getProp());
        checkConnections( connections.get(1).getValues(), new String[]{"test:f"});
        
        // Text search
        List<WNode> matches = source.search("aa");
        assertEquals(1, matches.size());
        checkConnections(matches, new String[]{"test:a"});

        matches = source.search("label");
        Set<WNode> matchset = new HashSet<>(matches);
        assertEquals(4, matchset.size());
        checkConnections(matches, new String[]{"test:i1", "test:i2", "test:i3", "test:i5"});
        
        matches = source.search("label", 2);
        assertTrue( matches.size() <= 2);  // Maybe only one if first two Lucene hits are for different props on same resource
        
        matches = source.search("pref");
        assertEquals(1, matches.size());
        checkConnections(matches, new String[]{"test:i1"});
    }
    
    @Test
    public void testQuery() {
        String q = "SELECT ?x WHERE {test:i1 ?p ?x}";
        List<Literal> literals = SQueryUtil.selectLiteralVar("x", q, ssource, app.getPrefixes());
        TestUtil.testArray(literals, new Literal[]{
                ResourceFactory.createPlainLiteral("name"),
                ResourceFactory.createPlainLiteral("rdfs label"),
                ResourceFactory.createPlainLiteral("Alt label"),
                ResourceFactory.createPlainLiteral("Pref label"),
        });
        
        q = PrefixUtils.expandQuery(q, app.getPrefixes());
        literals = ssource.selectVar(q, "x", Literal.class);
        TestUtil.testArray(literals, new Literal[]{
                ResourceFactory.createPlainLiteral("name"),
                ResourceFactory.createPlainLiteral("rdfs label"),
                ResourceFactory.createPlainLiteral("Alt label"),
                ResourceFactory.createPlainLiteral("Pref label"),
        });
        
        List<Resource> resources = SQueryUtil.selectResourceVar("x", "SELECT ?x WHERE {test:i1 ?p ?x}", ssource, app.getPrefixes());
        TestUtil.testArray(resources, new Resource[]{
                ResourceFactory.createResource(TEST_NS + "Sample")
        });
    }
    
    @Test
    public void testStreamableSelect() {
        String query = PrefixUtils.expandQuery("SELECT ?x WHERE {test:i1 a ?x}", app.getPrefixes());
        ClosableResultSet results = ssource.streamableSelect(query);
        try {
            assertTrue(results.hasNext());
            assertEquals(ResourceFactory.createResource(TEST_NS + "Sample"), results.next().getResource("x"));
            assertFalse(results.hasNext());
        } finally {
            results.close();
        }
    }
    
    @Test
    public void testUpdate() {
        assertTrue( ssource.isUpdateable() );
        String update = "" +
        		"PREFIX test: <http://www.epimorphics.com/vocabs/test/> \n" +
        		"DELETE {?x test:string ?s}\n" +
        		"INSERT {?x test:string 'new string'}\n" +
        		"WHERE {?x test:num 42; test:string ?s}";
        ssource.update( UpdateFactory.create(update) );
        
        WNode v = getNode("test:test").getPropertyValue("test:string");
        assertNotNull(v);
        assertEquals("new string", v.getLabel());
    }
    
    @Test
    public void testResourceView() {
        // Minimalist test, expand
        List<ResourceViewBase> views = ResourceViewFactory.getViews(ssource, "SELECT ?item WHERE {?item a test:Sample} ORDER BY ?item");
        assertEquals(5, views.size());
        assertEquals("Pref label", views.get(0).getLabel());
        assertEquals("Alt label", views.get(1).getLabel());
    }
    
    private void checkLabel(DatasetGraph dsg, String iN, String label) {
        Node i1 = NodeFactory.createURI(TEST_NS + iN);
        Graph g1 = dsg.getGraph(i1);
        assertNotNull(g1);
        assertTrue(g1.contains(i1, RDFS.label.asNode(), NodeFactory.createLiteral(label)));
    }
    
    private void checkConnections(List<WNode> ans, String[] expected) {
        WNode[] expectedN = new WNode[ expected.length ];
        for (int i = 0; i < expected.length; i++) {
            expectedN[i] = getNode( expected[i] );
        }
        TestUtil.testArray(ans, expectedN);
    }
}
