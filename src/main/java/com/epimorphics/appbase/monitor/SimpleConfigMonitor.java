/******************************************************************
 * File:        SimpleConfigMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  26 Jul 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;

/**
 * A simplified alternative to the general ConfigMonitor. 
 * Uses ConfigWatcher approach only, no support for scanners.
 * Subclasses should implement actions on add/modify/remove. 
 */
public abstract class SimpleConfigMonitor implements FileRecord.Process, Startup {
    static Logger log = LoggerFactory.getLogger( SimpleConfigMonitor.class );
    
    protected String monitorDirectory;
    protected Pattern filePattern = Pattern.compile(".*");
    
    /**
     * Set the directory monitor
     * @param dir
     */
    public void setDirectory(String dir) {
        monitorDirectory = dir;
    }
    
    /**
     * Set a regex pattern, only files whose name matches the pattern
     * will be acted on.
     */
    public void setFilePattern(String pattern) {
        filePattern = Pattern.compile(pattern);
    }
    
    @Override
    public void startup(App app) {
        try {
            ConfigWatcher.watch(new File(monitorDirectory), this);
        } catch (IOException e) {
            log.error("Failed to set watch on config director: " + monitorDirectory, e);
        }
    }
    
    public abstract void fileAdded(File file);
    public abstract void fileChanged(File file);
    public abstract void fileRemoved(File file);
    
    public void process(FileRecord record) {
        File file = record.getFile();
        if ( filePattern.matcher(file.getName()).matches() ) {
            switch(record.getState()) {
            case NEW:
                fileAdded(file);
                break;
            case MODIFIED:
                fileChanged(file);
                break;
            case DELETED:
                fileRemoved(file);
                break;
            }
        }
    }
}
