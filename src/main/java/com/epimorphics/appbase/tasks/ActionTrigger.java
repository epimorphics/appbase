/******************************************************************
 * File:        ActionTrigger.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.util.Map;

/**
 * Interface for a object that tests an event to see if it
 * should trigger the owning action. The default implementation
 * is a simple regex on the trigger name but could support
 * parameter matching or efficient trigger indexing if needed.
 */
public interface ActionTrigger {
    public static final String TRIGGER_KEY = "@trigger";

    public boolean matches(String trigger, Map<String, Object> parameters);
    
}
