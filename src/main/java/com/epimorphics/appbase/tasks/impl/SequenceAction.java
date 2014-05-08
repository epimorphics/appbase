/******************************************************************
 * File:        SequenceAction.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.NestedProgressReporter;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * Runs a set of component actions in sequence
 */
public class SequenceAction extends CompoundAction {

    @Override
    protected JsonObject doRun(JsonObject parameters, ProgressMonitorReporter monitor) {
        JsonObject result = new JsonObject();
        int n = componentActions.length;
        for (int i = 0; i < n; i++) {
            NestedProgressReporter prog = new NestedProgressReporter(monitor);
            result = componentActions[i].run(JsonUtil.merge(parameters, result), prog);
            if (! prog.succeeded()) {
                monitor.setFailed();
                return result;
            }
            monitor.setProgress((int)Math.floor(100*i/n));
        }
        monitor.setSucceeded();
        return result;
    }
    
}
