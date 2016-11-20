/******************************************************************
 * File:        UnionDatsetSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  25 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphMap;
import org.apache.jena.sparql.graph.GraphUnionRead;

/**
 * An in-memory source where the default model is the union of
 * the named models. Changes made to models other than via
 * the accessor or updater will not be visible in the 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class UnionDatasetSparqlSource extends DatasetSparqlSource {
    
    public UnionDatasetSparqlSource() {
        dataset = DatasetFactory.wrap( new UnionDatasetGraphMem() );
    }

    public static class UnionDatasetGraphMem extends DatasetGraphMap implements DatasetGraph {
        @Override
        public Graph getDefaultGraph() {
            return new GraphUnionRead(this);
        }
    }

    @Override
    public DatasetAccessor getAccessor() {
        if (accessor == null) {
            accessor = new LockingDatasetAccessor(dataset);
        }
        return accessor;
    }

}
