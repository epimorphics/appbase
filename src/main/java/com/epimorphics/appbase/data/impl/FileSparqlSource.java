/******************************************************************
 * File:        FileSPARQLSource.java
 * Created by:  Dave Reynolds
 * Created on:  27 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.util.FileManager;

/**
 * SparqlSource which serves a set of files from a single union
 * memory model, read only.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class FileSparqlSource extends ComponentBase implements SparqlSource {
    static Logger log = LoggerFactory.getLogger( FileSparqlSource.class );
    
    protected File dir;
    protected Model model;
    
    /**
     * Configuration call to set the base directory from which files are loaded.
     */
    public void setFileDir(String dirname) {
        dir = asFile(dirname);
        if (!dir.isDirectory() || !dir.canRead()) {
            throw new EpiException("Can't find/read file directory: " + dirname);
        }
    }
    
    public void startup(App app) {
        super.startup(app);
        model = ModelFactory.createDefaultModel();
        for (String file : dir.list(new RDFFileNames())) {
            FileManager.get().readModel(model, new File(dir, file).getPath());
            log.info("Loaded file: " + file);
        }
    }
    
    public static class RDFFileNames implements FilenameFilter {
        protected static final Pattern pattern = Pattern.compile(".*\\.(ttl|rdf|owl|nt|n3|xml)");
        
        @Override
        public boolean accept(File dir, String name) {
            return pattern.matcher(name).matches();
        }
        
    }

    @Override
    public ResultSet select(String queryString) {
        Query query = QueryFactory.create(queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        model.enterCriticalSection(true);
        try {
            return ResultSetFactory.makeRewindable( qexec.execSelect() );
        } finally { 
            qexec.close() ;
            model.leaveCriticalSection();
        }
    }

    @Override
    public Graph describeAll(String... uris) {
        Model description = ModelFactory.createDefaultModel();
        for (String uri: uris) {
            Closure.closure( model.createResource(uri), false, description);
        }
        return description.getGraph();
    }

    @Override
    public Graph[] describeEach(String... resources) {
        Graph[] graphs = new Graph[resources.length];
        for (int i = 0; i < resources.length; i++) {
            String uri = resources[i];
            graphs[i] = Closure.closure(model.createResource(uri), false).getGraph();
        }
        return graphs;
    }

    @Override
    public Graph construct(String queryString) {
        Query query = QueryFactory.create(queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        model.enterCriticalSection(true);
        try {
            Graph graph = new GraphMem();
            for (Iterator<Triple> i = qexec.execConstructTriples(); i.hasNext();) {
                graph.add(i.next());
            }
            return graph;
        } finally { 
            qexec.close() ;
            model.leaveCriticalSection();
        }
    }
}
