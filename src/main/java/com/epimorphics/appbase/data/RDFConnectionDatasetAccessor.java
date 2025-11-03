package com.epimorphics.appbase.data;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;

import java.util.function.Supplier;

public class RDFConnectionDatasetAccessor implements DatasetAccessor {
    public static RDFConnectionDatasetAccessor create(Supplier<RDFConnection> cxnFct) {
        return new RDFConnectionDatasetAccessor(cxnFct);
    }

    private final Supplier<RDFConnection> cxnFct;

    RDFConnectionDatasetAccessor(Supplier<RDFConnection> cxnFct) {
        this.cxnFct = cxnFct;
    }

    @Override
    public Model getModel() {
        try (RDFConnection cxn = cxnFct.get()) {
            return cxn.fetch();
        }
    }

    @Override
    public Model getModel(String graphUri) {
        try (RDFConnection cxn = cxnFct.get()) {
            return cxn.fetch(graphUri);
        }
    }

    @Override
    public void putModel(Model data) {
        try (RDFConnection cxn = cxnFct.get()) {
            cxn.put(data);
        }
    }

    @Override
    public void putModel(String graphUri, Model data) {
        try (RDFConnection cxn = cxnFct.get()) {
            cxn.put(graphUri, data);
        }
    }

    @Override
    public void deleteDefault() {
        try (RDFConnection cxn = cxnFct.get()) {
            cxn.delete();
        }
    }

    @Override
    public void deleteModel(String graphUri) {
        try (RDFConnection cxn = cxnFct.get()) {
            cxn.delete(graphUri);
        }
    }

    @Override
    public void add(Model data) {
        try (RDFConnection cxn = cxnFct.get()) {
            cxn.load(data);
        }
    }

    @Override
    public void add(String graphUri, Model data) {
        try (RDFConnection cxn = cxnFct.get()) {
            cxn.load(graphUri, data);
        }
    }
}
