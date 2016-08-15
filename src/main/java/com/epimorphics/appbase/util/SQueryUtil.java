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
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;

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
     * Return a list of all bindings of the given var as lexical forms of literals 
     */
    public static List<String> selectStringVar(String var, String query, SparqlSource source) {
        return asStringList( selectLiteralVar(var, query, source) );
    }

    /**
     * Return a list of all bindings of the given var as lexical forms of literals
     * Prefixes in the query will be expand from the given prefix mapping
     */
    public static List<String> selectStringVar(String var, String query, SparqlSource source, PrefixMapping pm) {
        return asStringList( selectLiteralVar(var, query, source, pm) );
    }
    
    private static List<String> asStringList(List<Literal> literals) {
        List<String> results = new ArrayList<>( literals.size() );
        for (Literal l : literals) {
            results.add( l.getLexicalForm() );
        }
        return results;
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
