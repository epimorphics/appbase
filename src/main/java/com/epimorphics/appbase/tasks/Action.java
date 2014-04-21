/******************************************************************
 * File:        Action.java
 * Created by:  Dave Reynolds
 * Created on:  18 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;


import java.util.Map;

import com.epimorphics.appbase.core.Named;
import com.epimorphics.appbase.monitor.ConfigInstance;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * A processing block that will be run asynchronously, reporting progress as it goes.
 */
public interface Action extends ConfigInstance, Named {
    
    /**
     * Run the action. This will normally be called from a separate worker thread.
     * 
     * @param parameters configuration parameters, both static and run specific
     * @param monitor the progress monitor through which the result can be reported.
     */
    public void run(Map<String, Object> parameters, ProgressMonitorReporter monitor);
    
    /**
     * Return the maximum time (in milliseconds) this action should be allowed to run.
     * Return -1 if there's no limit.
     */
    public int getTimeout();
    
    /**
     * Return an action that should be called if this action fails or times out
     * when run on the given set of parameters (or null if there is no such action)
     * @return an action name (String) or Action
     */
    public Object getOnError(Map<String, Object> parameters);
}
