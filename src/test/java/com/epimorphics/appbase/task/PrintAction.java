/******************************************************************
 * File:        PrintAction.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import java.util.Collections;
import java.util.Map;

import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.tasks.ProgressMonitorReporter;

public class PrintAction extends BaseAction {

    @Override
    protected Map<String, Object> doRun(Map<String, Object> parameters,
            ProgressMonitorReporter monitor) {
        monitor.report( getStringParameter(parameters, "message", "No message") );
        monitor.succeeded();
        return Collections.emptyMap();
    }

}
