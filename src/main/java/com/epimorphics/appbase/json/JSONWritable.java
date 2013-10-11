/******************************************************************
 * File:        JsonRepresentation.java
 * Created by:  Dave Reynolds
 * Created on:  2 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.json;


/**
 * Signature for API objects which self-serialize to JSON using
 * the Jena streaming JSON writers.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface JSONWritable {
    
    public void writeTo(JSFullWriter out);
    
}
