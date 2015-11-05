/******************************************************************
 * File:        FileSPARQLSource.java
 * Created by:  Dave Reynolds
 * Created on:  27 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.SparqlSource;

/**
 * SparqlSource which serves a set of files from a single union
 * memory model.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class FileSparqlSource extends DatasetSparqlSource implements SparqlSource {
    static Logger log = LoggerFactory.getLogger( FileSparqlSource.class );
    
    protected String fileSpec = "";
    
    /**
     * Configuration call to set file or directories from which to load,
     * can be a comma separated list
     */
    public void setFiles(String fileSpec) {
        this.fileSpec = fileSpec;
    }
    
    public void startup(App app) {
        super.startup(app);
        reload();
    }
    
    private void load(File f) {
        FileManager.get().readModel(dataset.getDefaultModel(), f.getPath());
        log.info("Loaded file: " + f);
    }
    
    /**
     * Reload the configured files and directories
     */
    public void reload() {
        // Clear old data to prevent bNode duplication
        dataset.getDefaultModel().removeAll();
        for (String fname : fileSpec.split(",")) {
            File f = asFile(fname);
            if (f.isDirectory()) {
                for (String file : f.list(new RDFFileNames())) {
                    load( new File(f, file) );
                }
            } else {
                load(f);
            }
        }
    }
    
    public static class RDFFileNames implements FilenameFilter {
        protected static final Pattern pattern = Pattern.compile(".*\\.(ttl|rdf|owl|nt|n3|xml)");
        
        @Override
        public boolean accept(File dir, String name) {
            return pattern.matcher(name).matches();
        }
        
    }

    
}
