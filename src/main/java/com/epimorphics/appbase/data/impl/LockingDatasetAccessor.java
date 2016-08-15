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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Provides transaction-safe version of DatasetAccessor for the given dataset.
 * Suitable for use with in memory datasets that use locks.
 */
public class LockingDatasetAccessor implements DatasetAccessor {
    protected Dataset dataset;
    protected DatasetAccessor wrapped;
    
    public LockingDatasetAccessor(Dataset dataset) {
        this.dataset = dataset;
        this.wrapped = DatasetAccessorFactory.create(dataset);
    }

    @Override
    public Model getModel() {
        dataset.getLock().enterCriticalSection(true);
        try {
            Model model = ModelFactory.createDefaultModel();
            model.add( wrapped.getModel() );
            return model;
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }

    @Override
    public Model getModel(String graphUri) {
        dataset.getLock().enterCriticalSection(true);
        try {
            Model model = ModelFactory.createDefaultModel();
            model.add( wrapped.getModel(graphUri) );
            return model;
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }

    @Override
    public boolean containsModel(String graphURI) {
        dataset.getLock().enterCriticalSection(true);
        try {
            return wrapped.containsModel(graphURI);
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }

    @Override
    public void putModel(Model data) {
        dataset.getLock().enterCriticalSection(false);
        try {
            wrapped.putModel(data);
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }

    @Override
    public void putModel(String graphUri, Model data) {
        dataset.getLock().enterCriticalSection(false);
        try {
            wrapped.putModel(graphUri, data);
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }

    @Override
    public void deleteDefault() {
        dataset.getLock().enterCriticalSection(false);
        try {
            wrapped.deleteDefault();
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }

    @Override
    public void deleteModel(String graphUri) {
        dataset.getLock().enterCriticalSection(false);
        try {
            wrapped.deleteModel(graphUri);
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }

    @Override
    public void add(Model data) {
        dataset.getLock().enterCriticalSection(false);
        try {
            wrapped.add(data);
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }

    @Override
    public void add(String graphUri, Model data) {
        dataset.getLock().enterCriticalSection(false);
        try {
            wrapped.add(graphUri, data);
        } finally {
            dataset.getLock().leaveCriticalSection();
        }
    }
    

}
