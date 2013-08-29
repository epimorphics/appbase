/******************************************************************
 * File:        RemoteSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  29 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import com.epimorphics.appbase.data.SparqlSource;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;

/**
 * Sparql source for querying remote sparql endpoints.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RemoteSparqlSource extends BaseSparqlSource implements SparqlSource {
    protected String endpoint;
    
    public void setEndpont(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    protected QueryExecution start(String queryString) {
        return QueryExecutionFactory.sparqlService(endpoint, queryString);
    }

    @Override
    protected void finish(QueryExecution qexec) {
        qexec.close();
    }

    
}
