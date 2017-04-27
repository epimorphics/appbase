/******************************************************************
 * File:        ConfigInstance.java
 * Created by:  Dave Reynolds
 * Created on:  19 Jan 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;


/**
 * Interface for things which can be dynamically configured
 * by a ConfigMonitor. Need some sort of ID/name by which the latest
 * version can be retrieved.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface ConfigInstance {

    /**
     * Return a name by which this instance can be
     * retrieved, should be unique across the configured instances.
     */
    public String getName();
    
    public void setName(String name);
    
}
