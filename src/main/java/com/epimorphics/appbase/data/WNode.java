/******************************************************************
 * File:        WNode.java
 * Created by:  Dave Reynolds
 * Created on:  26 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.rdfutil.NodeUtil;
import com.epimorphics.util.EpiException;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
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
    
    public Node asNode() {
        return node;
    }

    /**
     * Return true if this node is the start of an RDF list, possibly an empty one.
     */
    public boolean isList() {
        if (isLiteral()) {
            return false;
        }
        if (node.equals(RDF.nil.asNode())) {
            return true;
        }
        Graph g = ensureDescribed(); 
        return g.contains(node, RDF.first.asNode(), Node.ANY) && g.contains(node, RDF.rest.asNode(), Node.ANY);
    }

    /**
     * Assumes this is an RDF List and returns the list of members.
     * If the RDF list is not well formed that the subset of members returned may be arbirary.
     */
    public List<WNode> asList() {
        List<WNode> list = new ArrayList<>();
        Graph g = ensureDescribed();
        Node current = node;
        while (current != null && ! current.equals(RDF.nil.asNode())) {
            list.add( getNode( NodeUtil.getPropertyValue(current, RDF.first.asNode(), g) ) );
            current = NodeUtil.getPropertyValue(current, RDF.rest.asNode(), g);
        }
        return list;
    }
    
    // -- Label and description access -------------------------------
    
    protected Graph ensureLabelled() {
        if (description == null || !description.hasLabels()) {
            if (node.isURI()) {
                source.ensureLabeled(this);
            }
            if (description == null) {
                description = new NodeDescription(node, new GraphMem());
            }
        }
        return description.getGraph();
    }
    
    protected Graph ensureDescribed() {
        if (description == null || !description.isFullDescription()) {
            if (node.isURI()) {
                source.ensureDescribed(this);
            }
            if (description == null) {
                description = new NodeDescription(node, new GraphMem());
            }
        }
        return description.getGraph();
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
        
    protected boolean isDescribed(boolean fully) {
        if (!isURIResource()) {
            return true;
        }
        if (description != null) {
            return false;
        }
        return fully ? description.isFullDescription() : description.hasLabels() ;
    }
    
    protected void setDescription(NodeDescription description) {
        this.description = description;
    }
    
    // -- property values -----------
    
    public WNode getPropertyValue(Object prop) {
        Graph g = ensureDescribed();
        return getNode( NodeUtil.getPropertyValue(node, asNode(prop), g) );
    }
    
    protected Node asNode(Object prop) {
        if (prop instanceof Node) {
            return (Node)prop;
        } else if (prop instanceof RDFNode) {
            return ((RDFNode)prop).asNode();
        } else if (prop instanceof String) {
            return NodeFactory.createURI( source.getApp().getPrefixes().expandPrefix((String)prop) );
        } else {
            throw new EpiException("Illegal type used to define property: " + prop);
        }
    }
 
    public boolean hasResourceValue(Object prop, Object value) {
        Graph g = ensureDescribed();
        return g.contains(node, asNode(prop), asNode(value));
    }
    
    public List<WNode> listPropertyValues(Object prop) {
        Graph g = ensureDescribed();
        List<WNode> values = new ArrayList<>();
        ExtendedIterator<Triple> i = g.find(node, asNode(prop), Node.ANY);
        while (i.hasNext()) {
            values.add( getNode( i.next().getObject() ) );
        }
        i.close();
        return values;
    }
    
    public List<PropertyValue> listProperties() {
        Graph g = ensureDescribed();
        PropertyValueSet values = new PropertyValueSet();
        ExtendedIterator<Triple> i = g.find(node, Node.ANY, Node.ANY);
        while (i.hasNext()) {
            Triple t = i.next();
            values.add( getNode(t.getPredicate()), getNode(t.getObject()) );
        }
        i.close();
        return values.getOrderedValues();
    }
    
    protected WNode getNode(Node n) {
        WNode result = source.get(n);
        if (n.isBlank()) {
            result.setDescription( description );
        }
        return result;
    }
    
    // -- queries --------------------
    
    // listInLinks listInLinks(object)
    // listConnectedNodes

    // -- equality support based solely no underlying Node -------------------
    
    @Override
    public boolean equals(Object other) {
        return (other instanceof WNode) && (node.equals(((WNode)other).node));
    }
    
    @Override
    public int hashCode() {
        return node.hashCode();
    }
}
