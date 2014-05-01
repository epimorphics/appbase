/******************************************************************
 * File:        ParallelAction.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import java.util.Collections;
import java.util.Map;

import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.appbase.tasks.NestedProgressReporter;
import com.epimorphics.appbase.tasks.ActionManager.ActionExecution;
import com.epimorphics.tasks.ProgressMonitorReporter;

public class ParallelAction extends CompoundAction {
    protected ActionManager am;
    
    @Override
    public void resolve(ActionManager am) {
        super.resolve(am);
        this.am = am;
    }

    @Override
    protected Map<String, Object> doRun(Map<String, Object> parameters,
            ProgressMonitorReporter monitor) {
        int n = componentActions.length;
        ActionExecution[] aes = new ActionExecution[n];
        for (int i = 0; i < n; i++) {
            aes[i] = am.runAction(componentActions[i], parameters, new NestedProgressReporter(monitor));
        }
        boolean failed = false;
        for (int i = 0; i < n; i++) {
            aes[i].waitForCompletion();
            if (!aes[i].getMonitor().succeeded()) {
                failed = true;
            } else {
                monitor.setProgress((int)Math.floor(100*i/n));
            }
        }
        monitor.setSuccess( !failed );
        return Collections.emptyMap();
    }

}
