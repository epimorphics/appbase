/******************************************************************
 * File:        DatasetMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  24 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.apache.jena.riot.RDFDataMgr;

import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Monitors a directory of RDF files, loading each into a given SparqlSource as 
 * a separate graph with the graph URI "file:<filename>". Particularly useful
 * when the underlying store is a TDB store with default union set.
 */
public class DatasetMonitor extends ConfigMonitor<DatasetMonitor.MonitoredGraph> {
    protected SparqlSource source;
    protected DatasetAccessor accessor;
    
    public void setSparqlSource(SparqlSource source) {
        this.source = source;
    }
    
    public class MonitoredGraph implements ConfigInstance {
        File file;

        public MonitoredGraph(File file) {
            this.file = file;
        }
        
        @Override
        public String getName() {
            return "file:" + file.getName();
        }
        
        public String getFilepath() {
            return file.getPath();
        }
    }

    @Override
    protected Collection<MonitoredGraph> configure(File file) {
        return Collections.singletonList(new MonitoredGraph(file));
    }
    
    @Override
    protected void doAddEntry(MonitoredGraph entry) {
        super.doAddEntry(entry);
        Model model = RDFDataMgr.loadModel( entry.getFilepath() );
        getAccessor().putModel(entry.getName(), model);
    }

    protected void doRemoveEntry(MonitoredGraph entry) {
        super.doRemoveEntry(entry);
        getAccessor().deleteModel( entry.getName() );
    }

    protected DatasetAccessor getAccessor() {
        if (accessor == null) {
            if (source == null) {
                throw new EpiException("No sparql source configured for DatasetMonitor - " + this);
            }
            accessor = source.getAccessor();
            if (accessor == null) {
                throw new EpiException("Can't get data accessor for sparql source: " + source);
            }
        }
        return accessor;
    }
}
