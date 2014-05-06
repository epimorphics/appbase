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
import java.util.regex.Pattern;

import org.apache.jena.riot.RDFDataMgr;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.appbase.data.WSource;
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
    protected String baseDir;
    protected WSource  wsource;
    
    public void setSparqlSource(SparqlSource source) {
        this.source = source;
    }

    public SparqlSource getSource() {
        return source;
    }
    
    public WSource getWsource() {
        if (wsource == null) {
            wsource = new WSource();
            wsource.setSource(source);
            wsource.setName( getName() + "WS");
            wsource.startup( getApp() );
        }
        return wsource;
    }
    
    @Override
    public void startup(App app) {
        baseDir = scanDir.getPath() + File.separatorChar;
        super.startup(app);
    }
    
    public class MonitoredGraph implements ConfigInstance {
        String name;
        String path;

        public MonitoredGraph(File file) {
            path = file.getPath();
            name = "file:" + (path.startsWith(baseDir) ? path.substring(baseDir.length()): file.getName());
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        public String getFilepath() {
            return path;
        }
    }
    
    protected static final Pattern filePattern = Pattern.compile(".*\\.(ttl|rdf|owl|nt|n3)");
    
    @Override
    protected Collection<MonitoredGraph> configure(File file) {
        if ( filePattern.matcher(file.getName()).matches() ) {
            return Collections.singletonList(new MonitoredGraph(file));            
        } else {
            return Collections.emptyList();
        }
        
    }
    
    @Override
    protected void doAddEntry(MonitoredGraph entry) {
        try {
            Model model = RDFDataMgr.loadModel( entry.getFilepath() );
            getAccessor().putModel(entry.getName(), model);
            super.doAddEntry(entry);
            if (wsource != null) wsource.resetCache();
        } catch (Throwable t) {
            log.error("Failed add monitored graph: " + entry.getName(), t);
        }
    }

    protected void doRemoveEntry(MonitoredGraph entry) {
        getAccessor().deleteModel( entry.getName() );
        super.doRemoveEntry(entry);
        if (wsource != null) wsource.resetCache();
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
