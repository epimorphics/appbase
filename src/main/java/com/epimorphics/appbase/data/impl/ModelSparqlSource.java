/******************************************************************
 * File:        ModelSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;

import com.epimorphics.appbase.data.SparqlSource;

/**
 * Wrapper for a plain memory model so that it acts as a sparql source.
 * Uses the default critical section locking. Since it is a model, not a dataset,
 * it doesn't support getAccessor but does support updates. 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ModelSparqlSource extends BaseSparqlSource implements SparqlSource {
    protected Model model;
    protected DatasetGraph graphStore;
    
    public ModelSparqlSource() {
    }
    
    public ModelSparqlSource(Model model) {
        this.model = model;
    }
    
    public void setModel(Model model) {
        this.model = model;
    }

    @Override
    protected QueryExecution start(String queryString) {
        model.enterCriticalSection(true);
        return QueryExecutionFactory.create(queryString, model);
    }

    @Override
    protected void finish(QueryExecution qexec) {
        qexec.close();
        model.leaveCriticalSection();
    }
    

    @Override
    public boolean isUpdateable() {
        return true;
    }

    @Override
    public DatasetAccessor getAccessor() {
        return null;
    }
    

    @Override
    public void update(UpdateRequest update) {
        model.enterCriticalSection(true);
        UpdateExecutionFactory.create(update, getGraphStore()).execute();
        model.leaveCriticalSection();
    }
    
    protected DatasetGraph getGraphStore() {
        if (graphStore == null) {
            graphStore = DatasetGraphFactory.create(model.getGraph());
        }
        return graphStore;
    }        
}
