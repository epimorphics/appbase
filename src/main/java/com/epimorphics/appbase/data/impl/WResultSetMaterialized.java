/******************************************************************
 * File:        WResultSetMaterialized.java
 * Created by:  Dave Reynolds
 * Created on:  27 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.epimorphics.appbase.data.WQuerySolution;
import com.epimorphics.appbase.data.WResultSet;
import com.epimorphics.appbase.data.WSource;
import com.hp.hpl.jena.query.ResultSet;

/**
 * Implementation of WResultSet which materializes the full set of results
 * so it can be iteratored over multiple times.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class WResultSetMaterialized implements WResultSet {
    protected List<String> varnames;
    protected List<WQuerySolution> rows = new ArrayList<>();
    
    public WResultSetMaterialized(ResultSet results, WSource source) {
        varnames = results.getResultVars();
        while (results.hasNext()) {
            rows.add( new WQuerySolution(source, results.nextBinding()) );
        }
    }
    
    @Override
    public Iterator<WQuerySolution> iterator() {
        return rows.iterator();
    }

    @Override
    public List<String> getResultVars() {
        return varnames;
    }
    
    @Override
    public WResultSet copy() {
        return this;    
    }
    
    public int count() {
        return rows.size();
    }


}
