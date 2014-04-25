/******************************************************************
 * File:        UnionDatsetSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  25 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphMap;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.sparql.graph.GraphUnionRead;

/**
 * An in-memory source where the default model is the union of
 * the named models. Changes made to models other than via
 * the accessor or updater will not be visible in the 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class UnionDatasetSparqlSource extends DatasetSparqlSource {
    
    public UnionDatasetSparqlSource() {
        dataset = DatasetFactory.create( new UnionDatasetGraphMem() );
    }

    public static class UnionDatasetGraphMem extends DatasetGraphMap implements DatasetGraph {
        
        public UnionDatasetGraphMem() {
            super( GraphFactory.createDefaultGraph() );
        }
        
        @Override
        protected Graph getGraphCreate() {
            return GraphFactory.createDefaultGraph();
        }
        

        @Override
        public void addGraph(Node graphName, Graph graph)
        {
            super.addGraph(graphName, graph);
            rebuildUnion();
        }

        @Override
        public void removeGraph(Node graphName)
        {
            super.removeGraph(graphName);
            rebuildUnion();
        }
        
        // The "synchronized" is hopefully redundant, all calls ought to be from within dataset level criticalSections
        protected synchronized void rebuildUnion() {
            List<Node> graphs = new ArrayList<>();
            for (Iterator<Node> i = listGraphNodes(); i.hasNext(); ) {
                graphs.add( i.next() );
            }
            setDefaultGraph( new GraphUnionRead(this, graphs) );
        }

    }

}
