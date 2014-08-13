/******************************************************************
 * File:        ReportErrorAction.java
 * Created by:  Dave Reynolds
 * Created on:  13 Aug 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.tasks.ProgressMonitorReporter;

public class ReportErrorAction extends BaseAction {
    public static final String MESSAGE_PARAM = "message";
    
    public ReportErrorAction() {
        super();
    }
    
    public ReportErrorAction(String message) {
        super();
        setConfig(MESSAGE_PARAM, message);
    }
    
    @Override
    public String getName() {
        return "report error";
    }

    @Override
    protected JsonObject doRun(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        monitor.report( getStringParameter(parameters, MESSAGE_PARAM, "Unknown error") );
        monitor.setFailed();
        return parameters;
    }

}
