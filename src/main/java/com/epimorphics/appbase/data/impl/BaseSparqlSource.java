/******************************************************************
 * File:        BaseSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  29 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.util.Iterator;
import java.util.List;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.data.ClosableResultSet;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.appbase.util.SQueryUtil;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.Closure;

/**
 * Generic implementation of a sparql source.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public abstract class BaseSparqlSource extends ComponentBase implements SparqlSource {

    @Override
    public ResultSet select(String queryString) {
        QueryExecution qexec = start(queryString);
        try {
            return ResultSetFactory.makeRewindable( qexec.execSelect() );
        } finally { 
            finish(qexec);
        }
    }

    @Override
    public <T> List<T> selectVar(String queryString, String varname, Class<T> cls) {
        QueryExecution qexec = start(queryString);
        try {
            return SQueryUtil.resultsFor(qexec.execSelect(), varname, cls);
        } finally { 
            finish(qexec);
        }
    }

    @Override
    public ClosableResultSet streamableSelect(String query) {
        return new SSResultSet(this, query);
    }

    @Override
    public Graph describeAll(String... uris) {
        if (uris.length == 0) return null;
        
        StringBuffer query = new StringBuffer();
        query.append("DESCRIBE");
        for (String uri: uris) {
            query.append(" <" + uri + ">");
        }
        return describe(query.toString());
    }

    @Override
    public Graph describe(String query) {
        QueryExecution qexec = start(query);
        try {
            Graph graph = new GraphMem();
            for (Iterator<Triple> i = qexec.execDescribeTriples(); i.hasNext();) {
                graph.add(i.next());
            }
            return graph;
        } finally { 
            finish(qexec);
        }
    }
    
    @Override
    public Graph[] describeEach(String... uris) {
        Model all = ModelFactory.createModelForGraph( describeAll(uris) );
        Graph[] graphs = new Graph[ uris.length ];
        for (int i = 0; i < uris.length; i++) {
            graphs[i] = Closure.closure( all.createResource(uris[i]), false).getGraph();

        }
        return graphs;
    }

    @Override
    public Graph construct(String queryString) {
        QueryExecution qexec = start(queryString);
        try {
            Graph graph = new GraphMem();
            for (Iterator<Triple> i = qexec.execConstructTriples(); i.hasNext();) {
                graph.add(i.next());
            }
            return graph;
        } finally {
            finish(qexec);
        }
    }

    abstract protected QueryExecution start(String queryString);
    
    abstract protected void finish(QueryExecution qexec);
    
    public PrefixMapping getPrefixes() {
        return getApp().getPrefixes();
    }
}
