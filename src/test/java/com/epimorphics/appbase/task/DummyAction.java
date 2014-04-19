/******************************************************************
 * File:        DummyAction.java
 * Created by:  Dave Reynolds
 * Created on:  19 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.BindingEnv;
import com.epimorphics.tasks.ProgressReporter;

/**
 * Used for testing the action sub-system
 */
public class DummyAction implements Action {
    protected String name = "DummyAction";

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void run(BindingEnv parameters, ProgressReporter monitor) {
        monitor.report(getName() + " started");
        monitor.report("" + parameters.get("message"));
        for (int i = 1; i < ((Number)parameters.get("count", 0)).intValue(); i++){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
            monitor.report("" + parameters.get("message"));
        }
        monitor.report(getName() + " finished");
        
    }

    @Override
    public int getTimeout() {
        return 500;
    }

}
