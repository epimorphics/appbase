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
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Wrapper for a plain model so that it acts as a sparql source.
 * Uses the default critical section locking.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ModelSparqlSource extends BaseSparqlSource implements SparqlSource {
    protected Model model;
    
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
    
    
}
