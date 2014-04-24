/******************************************************************
 * File:        DatasetSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  24 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.appbase.data.SparqlSource;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * An in-memory source which supports graph access.
 * No built in initialization - subclass or initialize using a suitably ConfigMonitor.
 * @see FileSparqlSource
 */
public class DatasetSparqlSource extends BaseSparqlSource implements SparqlSource, Startup {
    static Logger log = LoggerFactory.getLogger( FileSparqlSource.class );
    
    protected Dataset dataset = DatasetFactory.createMem();
    protected GraphStore graphStore;
    protected DatasetAccessor accessor;
    protected String indexSpec = null;
    
    /**
     * Configuration call to enable text indexing of the loaded data.
     * Value should be "default" (to index rdfs:label) or a comma-separated list of predicates to index. These can
     * use curies with the prefixes as defined in the application's prefix service.
     * Index will always include rdfs:label.
     */
    public void setTextIndex(String indexSpec) {
        this.indexSpec = indexSpec;
    }
    
    @Override
    public void startup(App app) {
        super.startup(app);
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

    @Override
    public void update(UpdateRequest update) {
        dataset.getLock().enterCriticalSection(true);
        UpdateExecutionFactory.create(update, getGraphStore()).execute();
        dataset.getLock().leaveCriticalSection();
    }

    @Override
    public boolean isUpdateable() {
        return true;
    }

    @Override
    public DatasetAccessor getAccessor() {
        if (accessor == null) {
            accessor = DatasetAccessorFactory.create(dataset);
        }
        return accessor;
    }
    
    protected GraphStore getGraphStore() {
        if (graphStore == null) {
            graphStore = GraphStoreFactory.create(dataset);
        }
        return graphStore;
    }
    
    
}
