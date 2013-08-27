/******************************************************************
 * File:        WResultSetWrapper.java
 * Created by:  Dave Reynolds
 * Created on:  27 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.epimorphics.appbase.data.WQuerySolution;
import com.epimorphics.appbase.data.WResultSet;
import com.epimorphics.appbase.data.WSource;
import com.hp.hpl.jena.query.ResultSet;


/**
 * Implementation of WResultSet which simply wraps an underlying ResultSet.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class WResultSetWrapper implements Iterable<WQuerySolution>, Iterator<WQuerySolution>, WResultSet {
    protected ResultSet results;
    protected WSource source;
    
    public WResultSetWrapper(ResultSet results, WSource source) {
        this.results = results;
        this.source = source;
    }

    /** Get the variable names for the projection. Not all query
     *  solutions from a result have every variable defined. 
     */
    public List<String> getResultVars() {
        return results.getResultVars();
    }

    @Override
    public Iterator<WQuerySolution> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return results.hasNext();
    }

    @Override
    public WQuerySolution next() {
        return new WQuerySolution(source, results.nextBinding());
    }

    @Override
    public void remove() {
        throw new NotImplementedException();
    }
    
    @Override
    public WResultSet copy() {
        return new WResultSetMaterialized(results, source);
    }

}
