/******************************************************************
 * File:        WSource.java
 * Created by:  Dave Reynolds
 * Created on:  26 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.List;

import com.epimorphics.appbase.core.ComponentBase;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A wrapped SPARQL source, designed for easy use from UI scripting.
 * Uses app-wide prefix configuration to expand queries, provides caching
 * of resource descriptions to simplify use of remote sources.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class WSource extends ComponentBase {

    protected SparqlSource source;
    // TODO - caching
    
    public void setSource(SparqlSource source) {
        this.source = source;
    }
    
    public void setCacheSize(int size) {
        // TODO
    }
    
    /**
     * Run a SPARQL query on the data source.
     * 
     * @param query the query to be executed, prefix declarations will be added 
     * from the app-wide prefix settings.
     * @param bindings An array of Objects, which will be taken in pairs to be a string
     * var name and an object to encode as an RDF node.
     */
    public WResultSet select(String query, Object...bindings) {
        // TODO
        return null;
    }
    
    /**
     * Return a full description of a set of resources. The descriptions
     * may be returned from a cache or may result in a new query to the 
     * underlying source.
     * This batch call may be cheaper than repeated calls to the nodes themselves.
     */
    public List<WNode> describe(List<Resource> resources) {
        // TODO
        return null;
    }
    
    /**
     * Return a full description of a resource. The description
     * may be returned from a cache or may result in a new query to the 
     * underlying source.
     */
    public WNode describe(Resource resource) {
        // TODO
        return null;
    }
    
    /**
     * Ensure that each resource in a result set has an associated description.
     * This batch call may be cheaper than repeated calls to the nodes themselves.
     */
    public WResultSet describe(WResultSet results) {
        // TODO
        return null;
    }
    
    /**
     * Return a labelled version a set of resources. The label descriptions
     * may be returned from a cache or may result in a new query to the 
     * underlying source.
     * This batch call may be cheaper than repeated calls to the nodes themselves.
     */
    public List<WNode> label(List<Resource> resources) {
        // TODO
        return null;
    }
    
    /**
     * Return a labelled version of a resource. The label description
     * may be returned from a cache or may result in a new query to the 
     * underlying source.
     */
    public WNode label(Resource resource) {
        // TODO
        return null;
    }
    
    /**
     * Ensure that each resource in a result set has an associated label
     * This batch call may be cheaper than repeated calls to the nodes themselves.
     */
    public WResultSet label(WResultSet results) {
        // TODO
        return null;
    }
    
    protected Graph describe(Node node) {
        // TODO
        return null;
    }
    
    protected Graph label(Node node) {
        // TODO
        return null;
    }
    
}
