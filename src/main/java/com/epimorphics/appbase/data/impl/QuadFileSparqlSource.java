/******************************************************************
 * File:        QuadFileSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  16 Feb 2017
 * 
 * (c) Copyright 2017, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.impl.UnionDatasetSparqlSource.UnionDatasetGraphMem;

/**
 * Sparql source which serves an in-memory dataset intialized
 * from a quad file.
 */
public class QuadFileSparqlSource extends DatasetSparqlSource {
    static Logger log = LoggerFactory.getLogger( QuadFileSparqlSource.class );
    
    protected String fileSpec = "";
    protected boolean unionDefault = false;
    
    /**
     * Configuration call to define quad file to load
     */
    public void setFile(String fileSpec) {
        this.fileSpec = fileSpec;
    }
    
    /**
     * Configuration call to set union-default graph
     */
    public void setUnionDefault(boolean unionDefault) {
        this.unionDefault = unionDefault;
    }
    
    public void startup(App app) {
        super.startup(app);
        reload();
    }
    
    /**
     * Reload the configured files and directories
     */
    public void reload() {
        if (unionDefault) {
            dataset = DatasetFactory.wrap( new UnionDatasetGraphMem() );
        }
        RDFDataMgr.read(dataset, fileSpec);
        log.info("Loaded " + fileSpec);
    }

}
