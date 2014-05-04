/******************************************************************
 * File:        Record.java
 * Created by:  Dave Reynolds
 * Created on:  3 May 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import com.epimorphics.util.PrefixUtils;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;

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
     * Initialize the view given a source and a base URI.
     */
    public void init(SparqlSource source, String uri) {
        init(source, uri, null);
    }
    
    /**
     * Initialize the view
     */
    public void init(SparqlSource source, String uri, PrefixMapping prefixes) {
        Model model = ModelFactory.createModelForGraph( fetchDescription(source, uri) );
        if (prefixes == null) {
            prefixes = source.getPrefixes();
        }
        if (prefixes != null) {
            model.setNsPrefixes(prefixes);
        }
        root = model.getResource(uri);
    }

    /**
     * Construct a suitable view of the resource from the given source.
     * Subclasses should override this to provide desired custom views.
     */
    protected Graph fetchDescription(SparqlSource source, String uri) {
        return source.describeAll(uri);
    }
    
    protected static String expand(SparqlSource source, String query, String uri) {
        return PrefixUtils.expandQuery(query.replaceAll("\\%s", uri), source.getPrefixes());
    }
 
}
