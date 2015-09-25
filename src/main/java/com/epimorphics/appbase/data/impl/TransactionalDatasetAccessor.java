/******************************************************************
 * File:        TransactionalDatasetAccessor.java
 * Created by:  Dave Reynolds
 * Created on:  24 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Provides transaction-safe version of DatasetAccessor for the given dataset.
 * Suitable for use with TDB. Gets are made safe by creating an in-memory copy so
 * should normally be avoided in favour of selective query.
 */
public class TransactionalDatasetAccessor implements DatasetAccessor {
    protected Dataset dataset;
    protected DatasetAccessor wrapped;
    
    public TransactionalDatasetAccessor(Dataset dataset) {
        this.dataset = dataset;
        this.wrapped = DatasetAccessorFactory.create(dataset);
    }

    @Override
    public Model getModel() {
        dataset.begin(ReadWrite.READ);
        try {
            Model model = ModelFactory.createDefaultModel();
            model.add( wrapped.getModel() );
            return model;
        } finally {
            dataset.end();
        }
    }

    @Override
    public Model getModel(String graphUri) {
        dataset.begin(ReadWrite.READ);
        try {
            Model model = ModelFactory.createDefaultModel();
            model.add( wrapped.getModel(graphUri) );
            return model;
        } finally {
            dataset.end();
        }
    }

    @Override
    public boolean containsModel(String graphURI) {
        dataset.begin(ReadWrite.READ);
        try {
            return wrapped.containsModel(graphURI);
        } finally {
            dataset.end();
        }
    }

    @Override
    public void putModel(Model data) {
        dataset.begin(ReadWrite.WRITE);                 
        try {
            wrapped.putModel(data);
            dataset.commit();
        } finally {
            dataset.end();
        }
    }

    @Override
    public void putModel(String graphUri, Model data) {
        dataset.begin(ReadWrite.WRITE);                 
        try {
            wrapped.putModel(graphUri, data);
            dataset.commit();
        } finally {
            dataset.end();
        }
    }

    @Override
    public void deleteDefault() {
        dataset.begin(ReadWrite.WRITE);                 
        try {
            wrapped.deleteDefault();
            dataset.commit();
        } finally {
            dataset.end();
        }
    }

    @Override
    public void deleteModel(String graphUri) {
        dataset.begin(ReadWrite.WRITE);                 
        try {
            wrapped.deleteModel(graphUri);
            dataset.commit();
        } finally {
            dataset.end();
        }
    }

    @Override
    public void add(Model data) {
        dataset.begin(ReadWrite.WRITE);                 
        try {
            wrapped.add(data);
            dataset.commit();
        } finally {
            dataset.end();
        }
    }

    @Override
    public void add(String graphUri, Model data) {
        dataset.begin(ReadWrite.WRITE);                 
        try {
            wrapped.add(graphUri, data);
            dataset.commit();
        } finally {
            dataset.end();
        }
    }
    

}
