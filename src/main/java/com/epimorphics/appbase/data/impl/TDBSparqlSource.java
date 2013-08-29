/******************************************************************
 * File:        TDBSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  29 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.io.File;
import java.io.IOException;

import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A Sparql source which provides access to a TDB-based persistent
 * data store with an optional jena-text index.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TDBSparqlSource extends BaseSparqlSource implements SparqlSource {
    protected File tdbDir;
    protected File textIndex;
    protected Dataset dataset;
    protected boolean isUnionDefault;
    
    public void setLocation(String loc) {
        tdbDir = asFile(loc);
        if (!tdbDir.exists() || !tdbDir.canRead()) {
            throw new EpiException("Configured location for TDB source is not accessible: " + loc);
        }
    }
    
    public void setIndex(String index) {
        textIndex = asFile(index);
    }
    
    public void setUnionDefault(boolean flag) {
        isUnionDefault = flag;
    }
    
    public void startup(App app) {
        super.startup(app);
        dataset = TDBFactory.createDataset( tdbDir.getPath() );
        if (textIndex != null) {
            try {
                Directory dir = FSDirectory.open(textIndex);
                EntityDefinition entDef = new EntityDefinition("uri", "text", RDFS.label.asNode()) ;
                dataset = TextDatasetFactory.createLucene(dataset, dir, entDef) ;            
            } catch (IOException e) {
                throw new EpiException("Failed to create jena-text lucence index area", e);
            }
        }
    }
    
    @Override
    protected QueryExecution start(String queryString) {
        Query query = QueryFactory.create(queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, dataset) ;
        if (isUnionDefault) {
            qexec.getContext().set(TDB.symUnionDefaultGraph, true) ;
        }
        dataset.begin(ReadWrite.READ);
        return qexec;
    }
    
    @Override
    protected void finish(QueryExecution qexec) {
        qexec.close() ;
        dataset.end();
    }

}
