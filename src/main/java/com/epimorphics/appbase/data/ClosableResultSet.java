/******************************************************************
 * File:        ClosableResultSet.java
 * Created by:  Dave Reynolds
 * Created on:  13 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import org.apache.jena.query.ResultSet;

/**
 * Interface for ResultSets which have associated resources (e.g. a read lock)
 * which have to be released when the ResultSet is closed.
 */
public interface ClosableResultSet extends ResultSet {

    public void close();
    
}
