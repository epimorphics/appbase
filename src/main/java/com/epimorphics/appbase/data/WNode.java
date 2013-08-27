/******************************************************************
 * File:        WNode.java
 * Created by:  Dave Reynolds
 * Created on:  26 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Wraps up an RDF Node for ease of access from UI scripting.
 * Provides convenient access to labels, allows property
 * access using shortname strings, reports property
 * values grouped and ordered, supports convenient access to property paths.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */

// Implementation works at Node/Graph level to avoid requiring a model for
// every description in the cache. This may be premature optimization.

public class WNode {
    protected WSource source;
    protected Node node;
    protected NodeDescription description;
    
    public WNode(WSource source, RDFNode node) {
        this(source, node.asNode());
    }
    
    public WNode(WSource source, Node node) {
        this(source, node, null);
    }
    
    public WNode(WSource source, Node node, NodeDescription description) {
        this.source = source;
        this.node = node;
        this.description = description;
    }
    
    // -- Basic accessors -------------------------------
    
    public boolean isResource() {
        return node.isURI() || node.isBlank();
    }
    
    public Resource asResource() {
        if (node.isURI()) {
            return ResourceFactory.createResource(node.getURI());
        } else if (node.isBlank()) {
            return new ResourceImpl(node, null);
        } else {
            return null;
        }
    }
    
    public boolean isURIResource() {
        return node.isURI();
    }
    
    public String getURI() {
        return node.getURI();
    }
    
    /**
     * For a URI resource returns a short form string identifying the resource.
     * Normally a curie based on the app-wide prefix configuration.
     * Returns null for non-URI resources.
     */
    public String getID() {
        if (isURIResource()) {
            return source.getApp().getPrefixes().shortForm(getURI());
        }
        return null;
    }
    
    public boolean isAnon() {
        return node.isBlank();
    }
    
    public boolean isLiteral() {
        return node.isLiteral();
    }
    
    public Literal asLiteral() {
        return new LiteralImpl(node, null);
    }
    
    public boolean isNumber() {
        return node.isLiteral() ? node.getLiteralValue() instanceof Number : false;
    }

    public Number asNumber() {
        if (node.isLiteral()) {
            Object val = node.getLiteralValue();
            if (val instanceof Number) {
                return (Number)val;
            }
        }
        return null;
    }
    
    public long asInt() {
        Number val = asNumber();
        if (val != null) {
            return val.longValue();
        }
        throw new NumberFormatException();
    }
    
    public double asFloat() {
        Number val = asNumber();
        if (val != null) {
            return val.doubleValue();
        }
        throw new NumberFormatException();
    }
    
    public Object getLiteralValue() {
        return node.getLiteralValue();
    }
    
    public String getDatatype() {
        return source.getApp().getPrefixes().shortForm( node.getLiteralDatatypeURI() );
    }
    
    // TODO date support
    
    // -- Label and description access -------------------------------
    
    protected void ensureLabelled() {
        if (description == null || !description.hasLabels()) {
            description = source.label( node );
        }
    }
    
    protected void ensureDescribed() {
        if (description == null || !description.isFullDescription()) {
            description = source.describe( node );
        }
    }
    
    public static final Node[] labelProps = { SKOS.prefLabel.asNode(), SKOS.altLabel.asNode(), 
        RDFS.label.asNode(), DCTerms.title.asNode(), FOAF.name.asNode() };
    
    /**
     * Return a lexical label to use for the node. In the case of a URI node which is not
     * yet described then this will provide a cache check and possibly a query to find sufficient description.
     */
    public String getLabel() {
        if (isLiteral()) {
            return asLiteral().getLexicalForm();
        }
        ensureLabelled();
        return description.getStringValue(labelProps);
    }
    
    /**
     * Return a lexical label to use for the node, which matches the target language. In the case of a URI node which is not
     * yet described then this will provide a cache check and possibly a query to find sufficient description.
     */
    public String getLabel(String language) {
        if (isLiteral()) {
            return asLiteral().getLexicalForm();
        }
        ensureLabelled();
        return description.getLangMatchValue(language, labelProps);
    }
    
    // getLabel getLabel(language)
    // listInLinks listInLinks(object)
    // listConnectedNodes
    // asList isList
    // getPropertyValue  hasResourceValue   listPropertyValues  listProperties

}
