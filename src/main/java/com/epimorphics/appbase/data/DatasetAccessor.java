package com.epimorphics.appbase.data;

import org.apache.jena.rdf.model.Model;

/**
 * Replacement for the deprecated and removed org.apache.jena.query.DatasetAccessor.
 * See <a href="https://github.com/apache/jena/blob/b7771659567e75cb6fcbc8666bad5250a78ac22e/jena-arq/src/main/java/org/apache/jena/query/DatasetAccessor.java">here</a>
 * for the original.
 */
public interface DatasetAccessor {
    /**
     * Get the default model of a Dataset
     */
    public Model getModel();

    /**
     * Get a named model of a Dataset
     */
    public Model getModel(String graphUri);

    /**
     * Put (replace) the default model of a Dataset
     */
    public void putModel(Model data);

    /**
     * Put (create/replace) a named model of a Dataset
     */
    public void putModel(String graphUri, Model data);

    /**
     * Delete (which means clear) the default model of a Dataset
     */
    public void deleteDefault();

    /**
     * Delete a named model of a Dataset
     */
    public void deleteModel(String graphUri);

    /**
     * Add statements to the default model of a Dataset
     */
    public void add(Model data);

    /**
     * Add statements to a named model of a Dataset
     */
    public void add(String graphUri, Model data);
}

