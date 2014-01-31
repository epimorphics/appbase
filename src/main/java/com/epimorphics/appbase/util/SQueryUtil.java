/******************************************************************
 * File:        SQueryUtil.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.util;

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.util.PrefixUtils;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;

/**
 * Helpful functions for issue queries to sparql sources.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class SQueryUtil {

    /**
     * Return a list of all bindings of the given var 
     */
    public static List<RDFNode> selectVar(String var, String query, SparqlSource source) {
        return resultsFor(source.select(query), var, RDFNode.class);
    }

    /**
     * Return a list of all bindings of the given var.
     * Prefixes in the query will be expand from the given prefix mapping
     */
    public static List<RDFNode> selectVar(String var, String query, SparqlSource source, PrefixMapping pm) {
        return resultsFor(source.select( PrefixUtils.expandQuery(query, pm) ), var, RDFNode.class);
    }

    /**
     * Return a list of all bindings of the given var 
     */
    public static List<Resource> selectResourceVar(String var, String query, SparqlSource source) {
        return resultsFor(source.select(query), var, Resource.class);
    }

    /**
     * Return a list of all bindings of the given var.
     * Prefixes in the query will be expand from the given prefix mapping
     */
    public static List<Resource> selectResourceVar(String var, String query, SparqlSource source, PrefixMapping pm) {
        return resultsFor(source.select( PrefixUtils.expandQuery(query, pm) ), var, Resource.class);
    }

    /**
     * Return a list of all bindings of the given var 
     */
    public static List<Literal> selectLiteralVar(String var, String query, SparqlSource source) {
        return resultsFor(source.select(query), var, Literal.class);
    }

    /**
     * Return a list of all bindings of the given var.
     * Prefixes in the query will be expand from the given prefix mapping
     */
    public static List<Literal> selectLiteralVar(String var, String query, SparqlSource source, PrefixMapping pm) {
        return resultsFor(source.select( PrefixUtils.expandQuery(query, pm) ), var, Literal.class);
    }
    

    /**
     * Take a column from result set and extract it as a list of values of the 
     * given type (e.g. Resource, Literal or generic RDFNode.
     * Skips any non-matching results
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> resultsFor(ResultSet results, String varname, Class<T> cls) {
        List<T> resultList = new ArrayList<T>();
        while (results.hasNext()) {
            RDFNode result = results.nextSolution().get(varname);
            if (cls.isInstance(result)) {
                resultList.add( (T) result);
            }
        }
        return resultList;
    }

}
