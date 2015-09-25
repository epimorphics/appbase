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

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.util.EpiException;
import com.epimorphics.vocabs.SKOS;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.CollectionGraph;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

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

public class WNode implements Comparable<WNode> {
    protected WSource source;
    protected Node node;
    protected NodeDescription description;
    protected boolean selfDescribed;    
    
    public WNode(WSource source, RDFNode node) {
        this(source, node, true);
    }
    
    public WNode(WSource source, RDFNode node, boolean selfDescribed) {
        this(source, node.asNode());
        this.selfDescribed = selfDescribed;
        if (selfDescribed) {
            // Prevent live fetch of description information by declaring that the node is fully described by its containing local model
            description = new NodeDescription(this.node, node.getModel().getGraph());
        }
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
    
    protected void setSelfDescribed(boolean selfDescribed) {
        this.selfDescribed = selfDescribed;
    }
    
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
        if (isLiteral()) {
            return new LiteralImpl(node, null);
        } else {
            return null;
        }
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
        NodeDescription g = getDescription();
        return g.contains(node, RDF.first.asNode(), Node.ANY) && g.contains(node, RDF.rest.asNode(), Node.ANY);
    }

    /**
     * Assumes this is an RDF List and returns the list of members.
     * If the RDF list is not well formed that the subset of members returned may be arbirary.
     */
    public List<WNode> asList() {
        List<WNode> list = new ArrayList<>();
        NodeDescription d = getDescription();
        Node current = node;
        while (current != null && ! current.equals(RDF.nil.asNode())) {
            list.add( getNode( d.getPropertyValue(current, RDF.first.asNode()) ) );
            current = d.getPropertyValue(current, RDF.rest.asNode());
        }
        return list;
    }
    
    // -- Label and description access -------------------------------
    
    protected NodeDescription getDescription() {
        if (description == null) {
            if (node.isURI()) {
                source.ensureDescribed(this);
            }
            if (description == null) {
                description = new NodeDescription(node, new CollectionGraph());
            }
        }
        return description;
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
        String label = getDescription().getStringValue(labelProps);
        return label == null ? defaultLabel() : label;
    }
    
    private String defaultLabel() {
        if (isLiteral()) {
            return asLiteral().getLexicalForm();
        } else if (isURIResource()) {
            return RDFUtil.getLocalname( getURI() );
        } else {
            return "[ "+ node.getBlankNodeLabel() + "]";
        }
    }
    
    /**
     * Return a lexical label to use for the node, which matches the target language. In the case of a URI node which is not
     * yet described then this will provide a cache check and possibly a query to find sufficient description.
     */
    public String getLabel(String language) {
        if (isLiteral()) {
            return asLiteral().getLexicalForm();
        }
        String label = getDescription().getLangMatchValue(language, labelProps);
        return label == null ? defaultLabel() : label;
    }
        
    public boolean isDescribed() {
        if (!isURIResource()) {
            return true;
        }
        return description != null;
    }
    
    public void setDescription(NodeDescription description) {
        this.description = description;
    }
    
    /**
     * Clear the cache description which will force future calls
     * to 
     */
    public void resetDescription() {
        description = null;
    }
    
    // -- property values -----------
    
    public WNode getPropertyValue(Object prop) {
        return getNode( getDescription().getPropertyValue( asNode(prop) ) );
    }
    
    protected Node asNode(Object prop) {
        if (prop instanceof Node) {
            return (Node)prop;
        } if (prop instanceof WNode) {
            return ((WNode)prop).node;
        } else if (prop instanceof RDFNode) {
            return ((RDFNode)prop).asNode();
        } else if (prop instanceof String) {
            return NodeFactory.createURI( source.getApp().getPrefixes().expandPrefix((String)prop) );
        } else {
            throw new EpiException("Illegal type used to define property: " + prop);
        }
    }
 
    public boolean hasResourceValue(Object prop, Object value) {
        return getDescription().contains(node, asNode(prop), asNode(value));
    }
    
    public List<WNode> listPropertyValues(Object prop) {
        List<WNode> values = new ArrayList<>();
        ExtendedIterator<Triple> i = getDescription().find(node, asNode(prop), Node.ANY);
        while (i.hasNext()) {
            values.add( getNode( i.next().getObject() ) );
        }
        i.close();
        return values;
    }
    
    public List<PropertyValue> listProperties() {
        PropertyValueSet values = new PropertyValueSet();
        ExtendedIterator<Triple> i = getDescription().find(node, Node.ANY, Node.ANY);
        while (i.hasNext()) {
            Triple t = i.next();
            values.add( getNode(t.getPredicate()), getNode(t.getObject()) );
        }
        i.close();
        return values.getOrderedValues();
    }
    
    protected WNode getNode(Node n) {
        if (n == null) return null;
        if (n.isLiteral()) {
            return new WNode(source, n);
        } else if (n.isBlank() || selfDescribed) {
            if (description != null) {
                NodeDescription nd = new NodeDescription(n, description.description);
                WNode result = new WNode(source, n, nd);
                result.setSelfDescribed(selfDescribed);
                return result;
            }
        }
        return source.get(n);
    }
    
    // -- queries --------------------
    
    /**
     * Return the set of nodes which point this one via a specific property
     * Reconsults the source, even if there is a local description cache.
     */
    public List<WNode> listInLinks(Object prop) {
        return connectedNodes( "^<" + asNode(prop).getURI() + ">" );
    }
    
    /**
     * Return all nodes which are connected to this one via a SPARQL property path.
     * Reconsults the source, even if there is a local description cache.
     */
    public List<WNode> connectedNodes(String path) {
        if (!isURIResource()) return null;
        String query = String.format("SELECT ?x WHERE {<%s> %s ?x}", getURI(), path);
        List<WNode> results = new ArrayList<>();
        for (WQuerySolution row : source.select(query)) {
            results.add( row.get("x") );
        }
        return results;
    }
    
    /**
     * Return all the nodes which point this one via any property.
     * Reconsults the source, even if there is a local description cache.
     */
    public List<PropertyValue> listInLinks() {
        if (!isURIResource()) return null;
        String query = String.format("SELECT ?p ?x WHERE {?x ?p <%s>}", getURI());
        PropertyValueSet values = new PropertyValueSet();
        for (WQuerySolution row : source.select(query)) {
            values.add( row.get("p"), row.get("x") );
        }
        return values.getOrderedValues();
    }
    
    @Override
    public String toString() {
        return node.toString();
    }
    
    // -- equality support based solely no underlying Node -------------------
    
    @Override
    public boolean equals(Object other) {
        return (other instanceof WNode) && (node.equals(((WNode)other).node));
    }
    
    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public int compareTo(WNode other) {
        if (node.isLiteral()) {
            if (other.node.isLiteral()){
                return node.getLiteralLexicalForm().compareTo( other.node.getLiteralLexicalForm() );                
            } else {
                return -1;
            }
        }
        if (node.isURI()) {
            if (other.node.isURI()) {
                return node.getURI().compareTo(other.node.getURI());
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }
    
}
