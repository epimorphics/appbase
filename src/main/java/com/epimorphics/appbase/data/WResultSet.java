/******************************************************************
 * File:        WResultSet.java
 * Created by:  Dave Reynolds
 * Created on:  26 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.List;

/**
 * Interface to a set of wrapped SPARQL query results.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface WResultSet extends Iterable<WQuerySolution> {
    /** Get the variable names for the projection. Not all query
     *  solutions from a result have every variable defined. 
     */
    public List<String> getResultVars();
    
    /**
     * Return a materialized copy of the results
     */
    public WResultSet copy();
}
