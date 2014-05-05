/******************************************************************
 * File:        ResourceViewBase.java
 * Created by:  Dave Reynolds
 * Created on:  3 May 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.rdfutil.RDFUtil;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Provide convenience access functions for a Resource in some associated model. 
 * Simplifies scripting by allowing strings (URI, curie) as well as Properties for access.
 * Base for classes that provide application-specific views of the data
 */
public class ResourceView implements Comparable<ResourceView> {
    protected Resource root;
    protected String label;  // Cached since it is used a lot for sorting
    
    /**
     * Construct as a wrapper round an already retrieved resource whose associated
     * Model should contain all relevant information for the view and allow thread-safe access.
     */
    public ResourceView(Resource root) {
        this.root = root;
    }
    
    /**
     * Construct a null view, needs to be initialized via setRoot before use
     */
    public ResourceView() {
    }
    
    /**
     * Set the root of the view, the supplied root shoudld have an associated
     * Model containing all relevant information for the view and allow thread-safe access.
     */
    public void setRoot(Resource root) {
        this.root = root;
    }

    /**
     * URI of the root resource
     */
    public String getURI() {
        return root.getURI();
    }
    
    /**
     * Root resource, any modifications will only affect this in-memory view
     * and not the original source
     */
    public Resource asResource() {
        return root;
    }
    
    public Resource getRoot() {
        return root;
    }
    
    public Model getModel() {
        return root.getModel();
    }
    
    /**
     * Return a label for the resource, chosen from a built in set of plausible label properties
     */
    public String getLabel() {
        return RDFUtil.getLabel(root);
    }
    
    /**
     * Return a label for the resource, preferring the given language code, chosen from a built in set of plausible label properties
     */
    public String getLabel(String lang) {
        if (label == null) {
            label = RDFUtil.getLabel(root, lang); 
        }
        return label;
    }
    
    /**
     * Return a description for the resource, chosen from a built in set of plausible descriptio properties
     */
    public String getDescription() {
        return RDFUtil.getDescription(root);
    }
    
    /**
     * Return a description for the resource, preferring the given language code, chosen from a built in set of plausible descriptio properties
     */
    public String getDescription(String lang) {
        return RDFUtil.getDescription(root, lang);
    }

    protected Property asProperty(Object prop) {
        if (prop instanceof String) {
            Model m = root.getModel();
            return m.createProperty( m.expandPrefix((String)prop) );
        } else if (prop instanceof Property) {
            return (Property)prop;
        } else if (prop instanceof Resource) {
            return RDFUtil.asProperty((Resource)prop);
        } else {
            return null;
        }
    }

    protected Resource asResource(Object res) {
        if (res instanceof String) {
            Model m = root.getModel();
            return m.createResource( m.expandPrefix((String)res) );
        } else if (res instanceof Resource) {
            return (Resource)res;
        } else {
            return null;
        }
    }
    
    /**
     * Return true if the view root has a value of the given property
     */
    public boolean hasProperty(Object prop) {
        return root.hasProperty( asProperty(prop) );
    }
    
    /**
     * Return true if the view root has the given resource value for the property.
     */
    public boolean hasResourceValue(Object prop, Object resource) {
        return root.hasProperty(asProperty(prop), asResource(resource));
    }
    
    /**
     * Return a string value for the property of null if there is none
     */
    public String getStringValue(Object prop) {
        return RDFUtil.getStringValue(root, asProperty(prop));
    }

    /**
     * Return a string value for the property or the given default value
     */
    public String getStringValue(Object prop, String deflt) {
        return RDFUtil.getStringValue(root, asProperty(prop), deflt);
    }

    /**
     * Return the value of a resource on a property as a resource, or
     * null if there isn't a resource value.
     */
    public Resource getResourceValue(Object prop) {
        return RDFUtil.getResourceValue(root, asProperty(prop));
    }

    /**
     * Return all the resource values of the property on a resource.
     */
    public List<Resource> getResourceValues(Object prop) {
        return RDFUtil.getResourceValues(root, asProperty(prop));
    }

    /**
     * Answer the integer value of property p, or
     * ifAbsent if there isn't one.
     */
    public int getIntValue(Object p, int ifAbsent) {
        return RDFUtil.getIntValue(root, asProperty(p), ifAbsent);
    }

    /**
     * Answer the long value of property p or null if there isn't one.
     */
    public Long getLongValue(Object p) {
        return RDFUtil.getLongValue(root, asProperty(p));
    }

    /**
     * Answer the double value of property p or null if there isn't one.
     */
    public Double getDoubleValue(Object p) {
        return RDFUtil.getDoubleValue(root, asProperty(p));
    }

    /**
     * Answer the numeric value of property p or null if there isn't one.
     */
    public Number getNumericValue(Resource x, Property p) {
        return RDFUtil.getNumericValue(root, asProperty(p));
    }

    /**
        Answer the boolean value of property <code>p</code>. If there is no p-value, or the p-value is not a
        literal, return <code>ifAbsent</code>. Otherwise return true if
        the literal has spelling "true" or "yes", false if it has the
        spelling "false" or "no", and an unspecified value otherwise.
    */
    public boolean getBooleanValue(Object p, boolean ifAbsent ) {
        return RDFUtil.getBooleanValue(root, asProperty(p), ifAbsent);
    }

    /**
     * Return the value for the given property as a timestamp
     */
    public long getTimestampValue(Object p) {
        return RDFUtil.asTimestamp( root.getRequiredProperty( asProperty(p) ).getObject() );
    }
    
    /**
     * Return all literal values connected to the resource by a SPARQL path expression
     * (which can use prefixes registered with the underlying model which are typically
     * the system wide prefixes). Query just applies to the local view.
     */
    public List<Literal> getConnectedLiterals(String path) {
        return QueryUtil.connectedLiterals(root, path);
    }
    
    /**
     * Return all Resource values connected to the resource by a SPARQL path expression
     * (which can use prefixes registered with the underlying model which are typically
     * the system wide prefixes). Query just applies to the local view.
     */
    public List<Resource> getConnectedResource(String path) {
        return QueryUtil.connectedResources(root, path);
    }
    
    /**
     * Return all Resource values (as views) connected to the resource by a SPARQL path expression
     * (which can use prefixes registered with the underlying model which are typically
     * the system wide prefixes). Query just applies to the local view.
     */
    public List<ResourceView> getConnectedResourceViews(String path) {
        List<Resource> resources = getConnectedResource(path);
        List<ResourceView> result = new ArrayList<>( resources.size() );
        for (Resource r : resources) {
            result.add( new ResourceView(r) );
        }
        return result;
    }

    @Override
    public int compareTo(ResourceView o) {
        return getLabel().compareTo( o.getLabel() );
    }    
    
    @Override
    public String toString() {
//        return String.format("View[%s,%s]", root.getURI(), getLabel());
        return getLabel();
    }
       
}
