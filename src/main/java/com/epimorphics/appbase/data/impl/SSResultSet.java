/******************************************************************
 * File:        ResultSetExec.java
 * Created by:  Dave Reynolds
 * Created on:  13 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.util.List;

import com.epimorphics.appbase.data.ClosableResultSet;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/**
 * ResultSet which retains a reference to the query execution
 * which will be closed when the ResultSet is closed.
 */
public class SSResultSet implements ClosableResultSet {
    protected ResultSet results;
    protected QueryExecution qexec;
    protected BaseSparqlSource source;
    
    public SSResultSet(BaseSparqlSource source, String queryString) {
        this.source = source;
        qexec = source.start(queryString);
        results =  qexec.execSelect();
    }

    protected void doClose() {
        if (source != null) {
            source.finish(qexec);
            source = null;
        }
    }
    
    @Override
    public boolean hasNext() {
        if (source == null) return false; // Already finished and cleaned up
        boolean hasnext = results.hasNext();
        if (!hasnext) {
            doClose();
        }
        return hasnext;
    }

    @Override
    public QuerySolution next() {
        return results.next();
    }

    @Override
    public QuerySolution nextSolution() {
        return results.nextSolution();
    }

    @Override
    public Binding nextBinding() {
        return results.nextBinding();
    }

    @Override
    public int getRowNumber() {
        return results.getRowNumber();
    }

    @Override
    public List<String> getResultVars() {
        return results.getResultVars(); 
    }

    @Override
    public Model getResourceModel() {
        return results.getResourceModel();
    }

    @Override
    public void remove() {
        results.remove();
    }

    @Override
    public void close() {
        doClose();
    }
}
