/******************************************************************
 * File:        ResourceViewFactory.java
 * Created by:  Dave Reynolds
 * Created on:  3 May 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.util.EpiException;
import com.epimorphics.util.PrefixUtils;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Utilities for extracting views of resources from a SPARQL source
 */
public class ResourceViewFactory {

    /**
     * Construct a default view of the resource with the given uri
     */
    public static ResourceView getView(SparqlSource source, String uri) {
        ResourceViewBase view = new ResourceViewBase();
        view.init(source, uri);
        return view;
    }

    /**
     * Construct a view of the resource with the given uri as an isntance of the given class
     */
    public static <T extends ResourceViewBase>  T getView(SparqlSource source, String uri, Class<T> cls) {
        try {
            T view = cls.newInstance();
            view.init(source, uri);
            return view;
        } catch (Exception e) {
            throw new EpiException(e);
        }
    }
    
    /**
     * Runs the describe query to retrieve a view of the data an returns a list of ResourceViews one for
     * each resource which matches the select query (applied to the retrieved view)
     * @param source The source to query
     * @param describe describe query to return all data of relevance for the view
     * @param select   select query to find those resources within the description that should be returned
     * @param cls The class of objects to return 
     * @return
     */
    public static <T extends ResourceView> List<T> getViews(SparqlSource source, String describe, String select, Class<T> cls) { 
        Graph g = source.describe( PrefixUtils.expandQuery(describe, source.getPrefixes()) ); 
        Model m = ModelFactory.createModelForGraph(g);
        QueryExecution exec = QueryExecutionFactory.create(PrefixUtils.expandQuery(select, source.getPrefixes()), m);
        try {
            List<T> results = new ArrayList<>();
            ResultSet rs = exec.execSelect();
            while (rs.hasNext()) {
                T result = cls.newInstance();
                result.setRoot( rs.next().getResource("item") );
                results.add( result );
            }
            return results;
        } catch (Exception e) {
            throw new EpiException(e);
        } finally {
            exec.close();
        }
    }
    
    /**
     * Finds a list of resources using the given select query and returns a describe view of each
     * @param source The source to query
     * @param select The query to run which houls bind ?item variable only
     * @param cls The class of objects to return 
     * @return
     */
    public static <T extends ResourceView> List<T> getViews(SparqlSource source, String select, Class<T> cls) { 
        return getViews(source, select.replaceAll("(?i)SELECT", "DESCRIBE"), select, cls);
    }
    
    
    /**
     * Finds a list of resources using the given select query and returns a describe view of each
     * @param source The source to query
     * @param select The query to run which houls bind ?item variable only
     * @param cls The class of objects to return 
     * @return
     */
    public static List<ResourceView> getViews(SparqlSource source, String select) {
        return getViews(source, select.replaceAll("(?i)SELECT", "DESCRIBE"), select, ResourceView.class);
    }
    
    
    /**
     * Runs the describe query to retrieve a view of the data an returns a list of ResourceViews one for
     * each resource which matches the select query (applied to the retrieved view)
     * @param source The source to query
     * @param describe describe query to return all data of relevance for the view
     * @param select   select query to find those resources within the description that should be returned, should bind ?item variable
     * @param cls The class of objects to return 
     * @return
     */
    public static List<ResourceView> getViews(SparqlSource source, String describe, String select) {
        return getViews(source, describe, select, ResourceView.class);
    }

}
