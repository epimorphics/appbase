/******************************************************************
 * File:        NodeDescription.java
 * Created by:  Dave Reynolds
 * Created on:  27 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import com.epimorphics.rdfutil.NodeUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * A cachable description of an RDF node. 
 * Description itself is a Graph, though it might be a lightweight graph implementation.
 * Different levels of description might be in use, currently hardwire three categories.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class NodeDescription {
    protected Graph description;
    protected Node root;

    public NodeDescription(Node root, Graph description) {
        this.description = description;
        this.root = root;
    }

    // Subset of Graph interface needed for clients
    
    /** 
        Answer true iff the graph contains a triple matching (s, p, o).
        s/p/o may be concrete or fluid. Equivalent to find(s,p,o).hasNext,
        but an implementation is expected to optimise this in easy cases.
    */
    public boolean contains( Node s, Node p, Node o ) {
        return description.contains(s, p, o);
    }
    
    /** 
        Answer true iff the graph contains a triple that t matches; t may be
        fluid.
    */
    public boolean contains( Triple t ) {
        return description.contains(t);
    }
    
    /** Returns an iterator over Triple.
     */
    public ExtendedIterator<Triple> find(Node s,Node p,Node o) {
        return description.find(s, p, o);
    }
    

    // -- Utility functions to make it easier to work with Nodes ----
    
    public Node getPropertyValue(Node prop) {
        return NodeUtil.getPropertyValue(root, prop, description);
    }
    
    public Node getPropertyValue(Node subject, Node prop) {
        return NodeUtil.getPropertyValue(subject, prop, description);
    }
    
    /**
     * Return the lexical value of the first literal-valued property in the list of properties.
     * Or null if there is no matching property value.
     */
    public String getStringValue(Node... props){
        for (Node prop : props) {
            ExtendedIterator<Triple> i = description.find(root, prop, null);
            while (i.hasNext()) {
                Node val = i.next().getObject();
                if (val.isLiteral()) {
                    return val.getLiteralLexicalForm();
                }
            }
        }
        return null;
    }
    
    /**
     * Find the first property in the list which has a non-empty set of values
     * and return the value which best matches the target language string.
     */
    // TODO current implementation doesn't fulfil that contract, doesn't handle en-US v. en-GB
    public String getLangMatchValue(String language, Node... props) {
        for (Node prop : props) {
            ExtendedIterator<Triple> i = description.find(root, prop, null);
            if (i.hasNext()) {
                String lex = null;
                while (i.hasNext()) {
                    Node val = i.next().getObject();
                    if (val.isLiteral()) {
                        if (lex == null) {
                            // Keep first match as fall back
                            lex = val.getLiteralLexicalForm();
                        }
                        String lang = val.getLiteralLanguage();
                        if (lang.equalsIgnoreCase(language)) {
                            return val.getLiteralLexicalForm();
                        } else if (lang == null || lang.isEmpty()) {
                            lex = val.getLiteralLexicalForm();
                        } 
                    }
                }
                return lex;
            }
        }
        return null;
    }
    
    
}
