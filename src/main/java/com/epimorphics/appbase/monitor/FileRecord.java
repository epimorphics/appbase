/******************************************************************
 * File:        FileRecord.java
 * Created by:  Dave Reynolds
 * Created on:  22 Dec 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import java.io.File;

/**
 * Represents a change event on a file.
 */
public class FileRecord {
    
    public enum FileState { NEW, DELETED, MODIFIED };
    
    public interface Process {
        public void process(FileRecord record);
    }
    
    protected File file;
    protected FileState state;
    
    public FileRecord(File file, FileState state) {
        this.file = file;
        this.state = state;
    }

    public File getFile() {
        return file;
    }

    public FileState getState() {
        return state;
    }
    
    
}