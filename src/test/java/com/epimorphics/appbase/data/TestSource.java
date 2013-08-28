/******************************************************************
 * File:        TestSource.java
 * Created by:  Dave Reynolds
 * Created on:  28 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.List;

import org.junit.Test;

import com.epimorphics.util.TestUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.impl.LiteralLabelFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.vocabulary.RDFS;

import static org.junit.Assert.*;

public class TestSource extends BaseSourceTest {

    @Override
    protected String getTestFileDir() {
        return "src/test/data/source-tests/base";
    }

    @Test
    public void testBasics() {
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
    }
    
    private void checkLabel(DatasetGraph dsg, String iN, String label) {
        Node i1 = NodeFactory.createURI(TEST_NS + iN);
        Graph g1 = dsg.getGraph(i1);
        assertNotNull(g1);
        assertTrue(g1.contains(i1, RDFS.label.asNode(), NodeFactory.createLiteral(label)));
    }
    
}
