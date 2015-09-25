/******************************************************************
 * File:        WQuerySolution.java
 * Created by:  Dave Reynolds
 * Created on:  27 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

/**
 * Structure for returning result set rows from a wrapped sparql query.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */

// TODO should this be an interface with separate wrapped and materialized implementations?

public class WQuerySolution {
    protected Map<String, WNode> row;
    
    public WQuerySolution(Map<String, WNode> row) {
        this.row = row;
    }
    
    public WQuerySolution(WSource source, Binding binding) {
        row = new HashMap<String, WNode>();
        for (Iterator<Var> i = binding.vars(); i.hasNext();) {
            Var var = i.next();
            row.put(var.getName(), source.get(binding.get(var)));
        }
    }
    
    public WNode get(String varname) {
        return row.get(varname);
    }
    
    public Collection<String> varNames() {
        return row.keySet();
    }
}
