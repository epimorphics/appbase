/******************************************************************
 * File:        CachingDatasetMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  3 Nov 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Variant on the DatasetMonitor which maintains an up to date, in memory,
 * cache of the union of the monitored models. 
 * Only makes sense to use this when the underlying dataset is persistent and contains
 * other data, and the monitored data is small and needs fast access.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CachingDatasetMonitor extends DatasetMonitor {
    protected Map<String, Model> cachedModels = new HashMap<String, Model>();
    protected Model cachedUnion;
    
    public synchronized Model getCachedUnion() {
        if (cachedUnion == null) {
            cachedUnion = ModelFactory.createDefaultModel();
            for (Model model : cachedModels.values()) {
                cachedUnion.add(model);
            }
        }
        return cachedUnion;
    }
    
    @Override 
    protected synchronized void addModelHook(String graph, Model model) {
        cachedUnion = null;
        cachedModels.put(graph, model);
    }
    
    @Override
    protected synchronized void removeModelHook(String graph) {
        cachedUnion = null;
        cachedModels.remove(graph);
    }

}
