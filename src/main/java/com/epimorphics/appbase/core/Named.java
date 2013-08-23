/******************************************************************
 * File:        Named.java
 * Created by:  Dave Reynolds
 * Created on:  23 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

/**
 * Signature of components which have an identifying name.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface Named {
    public String getName();
    public void setName(String name);
}
