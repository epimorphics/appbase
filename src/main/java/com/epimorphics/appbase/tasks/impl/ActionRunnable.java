/******************************************************************
 * File:        ActionRunnable.java
 * Created by:  Dave Reynolds
 * Created on:  12 Aug 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * A simple action implementation which is a wrapper round
 * a java runnable class.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ActionRunnable extends BaseAction implements Action {
    protected Runnable runnable;
    
    public ActionRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    protected JsonObject doRun(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        try {
            runnable.run();
        } catch (Exception e) {
            monitor.reportError("Runnable action through error: " + e);
        }
        return JsonUtil.emptyObject();
    }
}
