/******************************************************************
 * File:        Record.java
 * Created by:  Dave Reynolds
 * Created on:  3 May 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import static com.epimorphics.util.PrefixUtils.expandQuery;

import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;

/**
 * Provides an in-memory, temporary, read-only view of some set of data from a SparqlSource. Rooted at a URI resource
 * it may include information on other related resources.
 * <p>
 * The base class provides some generic convenience access methods. 
 * Designed to allow subclasses to provide resource-specific convenience methods
 * and to modify the view fetched.</p>
 * <p>
 * Provides an alternative approach to WNode with more control of the view and no hidden fetching.
 * Like WNode provides scripting convenience so that you can use (curie) strings to provide properties to queries.  
 * </p>
 */
public class ResourceViewBase extends ResourceView {
    protected SparqlSource source;
    
    /**
     * Construct as an empty view which will need to be init'ed before use
     */
    public ResourceViewBase() {
        super();
    }
    
    /**
     * Construct as a wrapper round an already retrieved resource whose associated
     * Model should contain all relevant information for the view and allow thread-safe access.
     */
    public ResourceViewBase(Resource root) {
        super(root);
    }
    
    /**
     * Construct as a wrapper round an already retrieved resource whose associated
     * Model should contain all relevant information for the view and allow thread-safe access.
     */
    public ResourceViewBase(Resource root, SparqlSource source) {
        super(root);
        this.source = source;
    }
    
    /**
     * Construct as a wrapper round an already retrieved resource whose associated
     * Model should contain all relevant information for the view and allow thread-safe access.
     */
    public ResourceViewBase(Resource root, ResourceViewBase parent) {
        super(root);
        this.source = parent.getSource();
    }
    

    /**
     * Initialize the view given a source and a base URI.
     */
    public void init(SparqlSource source, String uri) {
        init(source, uri, null);
    }
    
    /**
     * Initialize the view
     */
    public void init(SparqlSource source, String uri, PrefixMapping prefixes) {
        this.source = source;
        Model model = ModelFactory.createModelForGraph( fetchDescription(source, uri) );
        if (prefixes == null) {
            prefixes = source.getPrefixes();
        }
        if (prefixes != null) {
            model.setNsPrefixes(prefixes);
        }
        root = model.getResource(uri);
    }
    
    public SparqlSource getSource() {
        return source;
    }

    public void setSource(SparqlSource source) {
        this.source = source;
    }

    /**
     * Construct a suitable view of the resource from the given source.
     * Subclasses should override this to provide desired custom views.
     */
    protected Graph fetchDescription(SparqlSource source, String uri) {
        return source.describeAll(uri);
    }
 
    /**
     * Construct a list views related to this view. In the queries any occurrence of the
     * string ?this will be replaced by a reference to this resource.
     * @param describe describe query to run on the sparql source to fetch sum of the required views
     * @param query select query to run on the returned description to extract the intended resources
     * @param viewType the type of resource view to construct
     * @return a list of views of the requested type which share the underlying model returned by the describe
     */
    public <T extends ResourceViewBase> List<T> listViews(String describe, String query, Class<T> viewType) {
        return ResourceViewFactory.getViews(getSource(), injectURI(describe), injectURI(query), viewType);
    }
    
    /**
     * Return a view of the resource value of the given property. This view is constructed by
     * going by the the source and requesting a full view, c.f. getConnectedView(s)
     */
    public <T extends ResourceViewBase> T getResourceView(Object property, Class<T> viewType) {
        Resource r = getResourceValue(property);
        if (r != null) {
            return ResourceViewFactory.getView(getSource(), r.getURI(), viewType);
        } else {
            return null;
        }
    }
    
    protected String injectURI(String q) {
        return q.replaceAll("\\?this", "<" + getURI() + ">");
    }
    
    protected String expand(String q, String uri) {
        return expandQuery(q, source.getPrefixes()).replaceAll("\\?this", "<" + uri + ">");
    }
    
    /**
     * Return select results from underlying source rather than view
     */
    public ResultSet fullSelect(String query) {
        return source.select( expand(query, getURI() ) );
    }
    
    /**
     * Return select results from underlying source rather than view
     */
    public QuerySolution fullSelectOne(String query) {
        ResultSet results = fullSelect(query);
        if (results.hasNext()) {
            return results.next();
        } else {
            return null;
        }
    }
}
