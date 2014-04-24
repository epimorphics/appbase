/******************************************************************
 * File:        ModelSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import com.epimorphics.appbase.data.SparqlSource;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Wrapper for a plain memory model so that it acts as a sparql source.
 * Uses the default critical section locking. Since it is a model, not a dataset,
 * it doesn't support getAccessor but does support updates. 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ModelSparqlSource extends BaseSparqlSource implements SparqlSource {
    protected Model model;
    protected GraphStore graphStore;
    
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
    
    protected GraphStore getGraphStore() {
        if (graphStore == null) {
            graphStore = GraphStoreFactory.create(model);
        }
        return graphStore;
    }        
}
