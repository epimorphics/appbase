/******************************************************************
 * File:        PrintAction.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import static com.epimorphics.json.JsonUtil.EMPTY_OBJECT;
import static com.epimorphics.json.JsonUtil.getStringValue;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.tasks.ProgressMonitorReporter;

public class PrintAction extends BaseAction {

    @Override
    protected JsonObject doRun(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        monitor.report( getStringValue(parameters, "message", "No message") );
        monitor.succeeded();
        return EMPTY_OBJECT;
    }

}
