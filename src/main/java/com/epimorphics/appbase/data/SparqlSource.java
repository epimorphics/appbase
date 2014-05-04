/******************************************************************
 * File:        SSource.java
 * Created by:  Dave Reynolds
 * Created on:  23 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.List;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Signature for a SPARQL data source. Provides an abstraction for both local datasets
 * and remote services. 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface SparqlSource {
    
    /**
     * Execute a sparql select query returning a local, safe copy of the results.
     * Any associated transaction will have been closed.
     */
    public ResultSet select(String query);
    
    /**
     * Execute a sparql select query returning a possibly streamable result set.
     * The result set must be closed to free any associated resources (e.g. an HTTP connection
     * to a remote source) and release any read transaction.
     */
    public ClosableResultSet streamableSelect(String query);

    /**
     * Execute a sparql select query returning a list of values for a single project variable in the query.
     * Useful to avoid a redundant copy of a ResultSet out of the transaction.
     * @param query the query to issue
     * @param varname the name of the variable to be returned, must one of the projected variables in the query
     * @param cls the class of values expected (Literal, Resource or RDFNode)
     */
    public <T> List<T> selectVar(String query, String varname, Class<T> cls);
    
    /**
     * Fetch the result of a full specified describe query
     */
    public Graph describe(String query);
    
    /**
     * Fetch a description of a set of resources.
     */
    public Graph describeAll(String... uris);

    /**
     * Fetch a description of a set of resources.
     */
    public Graph[] describeEach(String... uris);

    /**
     * Excecute a construct query
     */
    public Graph construct(String query);
    
    /**
     * A local name for the source (the component name in the configuration)
     */
    public String getName();
    
    /**
     * Perform an SPARQL update if the source allows this, throw a run
     * time exception if not.
     */
    public void update(UpdateRequest update);
    
    /**
     * Return true of the source supports update
     */
    public boolean isUpdateable();
    
    /**
     * Return an accessor through which graph level updates
     * can be performed (equivalent to access to the RESTful graph store API)
     * Returns null of no such update is supported
     */
    public DatasetAccessor getAccessor();
    
    /**
     * Return a set of prefix mappings to use with this source. 
     * Typically default or app wide mappings
     */
    public PrefixMapping getPrefixes();
}
