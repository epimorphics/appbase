/******************************************************************
 * File:        ComponentBase.java
 * Created by:  Dave Reynolds
 * Created on:  22 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

/**
 * Optional base class for components. A component can be any Java Bean
 * so this is base class is not required. Provides local pointer to the
 * app which contains this component.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ComponentBase implements Startup {
    protected App app;

    @Override
    public void startup(App app) {
        this.app = app;
    }
    
}
