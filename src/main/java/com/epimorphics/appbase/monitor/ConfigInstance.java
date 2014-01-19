/******************************************************************
 * File:        ConfigInstance.java
 * Created by:  Dave Reynolds
 * Created on:  19 Jan 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import java.io.File;

/**
 * Interface for things which can be dynamically configured
 * by a ConfigMonitor. They have to support access to a config
 * source file to enable new versions to be retrieved.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface ConfigInstance {

    /**
     * Return the configuration file which defined this instance.
     */
    public File getSourceFile();

    /**
     * Record the configuration file which defined this instance.
     */
    public void setSourceFile(File file);
    
}
