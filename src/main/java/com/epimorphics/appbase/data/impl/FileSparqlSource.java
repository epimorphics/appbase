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
import java.util.regex.Pattern;

import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.SparqlSource;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * SparqlSource which serves a set of files from a single union
 * memory model, read only.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class FileSparqlSource extends BaseSparqlSource implements SparqlSource {
    static Logger log = LoggerFactory.getLogger( FileSparqlSource.class );
    
    protected String fileSpec = "";
    protected String indexSpec = null;
    protected Dataset dataset;
    
    /**
     * Configuration call to set file or directories from which to load,
     * can be a comma separated list
     */
    public void setFiles(String fileSpec) {
        this.fileSpec = fileSpec;
    }
    
    /**
     * Configuration call to enable text indexing of the loaded data.
     * Value should be "default" (to index rdfs:label) or a comma-separated list of predicates to index. These can
     * use curies with the prefixes as defined in the application's prefix service.
     * Index will always include rdfs:label.
     */
    public void setTextIndex(String indexSpec) {
        this.indexSpec = indexSpec;
    }
    
    public void startup(App app) {
        super.startup(app);
        reload();
    }
    
    private void load(File f) {
        FileManager.get().readModel(dataset.getDefaultModel(), f.getPath());
        log.info("Loaded file: " + f);
    }
    
    /**
     * Reload the configured files and directories
     */
    public void reload() {
        dataset = DatasetFactory.createMem();
        if (indexSpec != null) {
            Directory dir = new RAMDirectory();
            // We have a choice of a single index of all predicates or separate indexes, could make this configurable
            // Currently default to a single joint index
            EntityDefinition entDef = new EntityDefinition("uri", "text", RDFS.label.asNode()) ;
            for (String spec : indexSpec.split(",")) {
                String uri = getApp().getPrefixes().expandPrefix(spec.trim());
                if ( ! uri.equals("default") ) {
                    Node predicate = NodeFactory.createURI(uri);
                    if (!predicate.equals(RDFS.label.asNode())) {
                        entDef.set("text", predicate);
                    }
                }
            }
            // Alterantive would be
//            for (String spec : indexSpec.split(",")) {
//                String uri = getApp().getPrefixes().expandPrefix(spec.trim());
//                if ( ! uri.equals("default") ) {
//                    Node predicate = NodeFactory.createURI(uri);
//                    entDef.set(NameUtils.safeName(predicate.getURI()), predicate);
//                }
//            }
            
            dataset = TextDatasetFactory.createLucene(dataset, dir, entDef) ;
        }
        for (String fname : fileSpec.split(",")) {
            File f = asFile(fname);
            if (f.isDirectory()) {
                for (String file : f.list(new RDFFileNames())) {
                    load( new File(f, file) );
                }
            } else {
                load(f);
            }
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
    public Graph describeAll(String... uris) {
        dataset.getLock().enterCriticalSection(true);
        Model description = ModelFactory.createDefaultModel();
        for (String uri: uris) {
            Closure.closure( dataset.getDefaultModel().createResource(uri), false, description);
        }
        dataset.getLock().leaveCriticalSection();
        return description.getGraph();
    }

    @Override
    public Graph[] describeEach(String... resources) {
        dataset.getLock().enterCriticalSection(true);
        Graph[] graphs = new Graph[resources.length];
        for (int i = 0; i < resources.length; i++) {
            String uri = resources[i];
            graphs[i] = Closure.closure( dataset.getDefaultModel().createResource(uri), false).getGraph();
        }
        dataset.getLock().leaveCriticalSection();
        return graphs;
    }
    
    @Override
    protected QueryExecution start(String queryString) {
        Query query = QueryFactory.create(queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, dataset) ;
        dataset.getLock().enterCriticalSection(true);
        return qexec;
    }
    
    @Override
    protected void finish(QueryExecution qexec) {
        qexec.close() ;
        dataset.getLock().leaveCriticalSection();
    }
    
}
