/******************************************************************
 * File:        Startup.java
 * Created by:  Dave Reynolds
 * Created on:  22 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

/**
 * Signature for a component that should be run when the owning App
 * starts up.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface Startup {
    
    public void startup(App app);
    
}
