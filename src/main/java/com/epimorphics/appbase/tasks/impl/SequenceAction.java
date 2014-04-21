/******************************************************************
 * File:        SequenceAction.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import java.util.Map;

import com.epimorphics.appbase.tasks.NestedProgressReporter;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * Runs a set of component actions in sequence
 */
public class SequenceAction extends CompoundAction {

    @Override
    protected void doRun(Map<String, Object> parameters, ProgressMonitorReporter monitor) {
        int n = componentActions.length;
        for (int i = 0; i < n; i++) {
            NestedProgressReporter prog = new NestedProgressReporter(monitor);
            componentActions[i].run(parameters, prog);
            if (! prog.succeeded()) {
                monitor.setFailed();
                return;
            }
            monitor.setProgress((int)Math.floor(100*i/n));
        }
        monitor.setSucceeded();
    }
    
}
