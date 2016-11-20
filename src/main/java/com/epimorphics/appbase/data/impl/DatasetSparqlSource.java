/******************************************************************
 * File:        DatasetSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  24 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.util.Closure;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDFS;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.appbase.data.SparqlSource;

/**
 * An in-memory source which supports graph access.
 * No built in initialization - subclass or initialize using a suitably ConfigMonitor.
 * @see FileSparqlSource
 */
public class DatasetSparqlSource extends BaseSparqlSource implements SparqlSource, Startup {
    static Logger log = LoggerFactory.getLogger( FileSparqlSource.class );
    
    protected Dataset dataset = DatasetFactory.createGeneral();
    protected DatasetGraph graphStore;
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
            
            dataset = TextDatasetFactory.createLucene(dataset, dir, entDef, new StandardAnalyzer(org.apache.jena.query.text.TextIndexLucene.VER)) ;
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
    
    protected DatasetGraph getGraphStore() {
        if (graphStore == null) {
            graphStore = DatasetGraphFactory.cloneStructure(dataset.asDatasetGraph());
        }
        return graphStore;
    }
    
    
}
