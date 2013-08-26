/******************************************************************
 * File:        SSource.java
 * Created by:  Dave Reynolds
 * Created on:  23 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.Collection;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.ResultSet;

/**
 * Signature for a SPARQL data source. Provides an abstraction for both local datasets
 * and remote services. 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface SparqlSource {
    
    /**
     * Execute a sparql select query. It is up to the implementation
     * to ensure appropriate read locks around the data source. This
     * might typically mean that the result set is copied rather than streamed.
     */
    public ResultSet select(String query);

    /**
     * Fetch a description of a set of resources.
     */
    public Graph describeAll(Collection<String> uris);

    /**
     * Fetch a description of a set of resources.
     */
    public Collection<Graph> describeEach(Collection<String> resources);
}
