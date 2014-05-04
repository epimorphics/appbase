/******************************************************************
 * File:        TestSourceBase.java
 * Created by:  Dave Reynolds
 * Created on:  28 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import org.junit.Before;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.PrefixService;
import com.epimorphics.appbase.data.impl.FileSparqlSource;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;

/**
 * Base utilities for testing the data wrappers - WSource et al.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public abstract class BaseSourceTest {
    public static final String SOURCE_NAME = "wsource";
    
    public static final String TEST_NS = "http://www.epimorphics.com/vocabs/test/";
    
    protected WSource source;
    protected SparqlSource ssource;
    protected App app;
    
    protected abstract String getTestFileDir();
    
    @Before
    public void setup() {
        app = new App("testing");
        
        PrefixService prefixes = new PrefixService();
        prefixes.setPrefixFile( "src/test/data/prefixes.ttl" );
        app.addComponent("prefixes", prefixes);
//        prefixes.startup(app);
        
        FileSparqlSource ss = new FileSparqlSource();
        ss.setFiles( getTestFileDir() );
//        ss.setTextIndex("default");
        ss.setTextIndex("rdfs:label, skos:altLabel, skos:prefLabel");
        ssource = ss;
        
        source = new WSource();
        source.setName(SOURCE_NAME);
        source.setSource(ss);

        app.addComponent("prefixes", prefixes);
        app.addComponent(SOURCE_NAME, source);
        app.addComponent("ssource", ss);
        app.startup();
    }
    
    public WNode getNode(String id) {
        Node node = NodeFactory.createURI( app.getPrefixes().expandPrefix(id) );
        return source.get(node);
    }
}
