/******************************************************************
 * File:        RecordingAction.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * Test action which record a series of messages sent to any instance
 */
public class RecordingAction extends BaseAction {
    protected static List<String> messages = new ArrayList<>();
    
    public static List<String> getMessages() {
        synchronized (messages) {
            return new ArrayList<String>( messages );
        }
    }
    
    public static void reset() {
        messages = new ArrayList<>();
    }

    @Override
    protected Map<String, Object> doRun(Map<String, Object> parameters, ProgressMonitorReporter monitor) {
        synchronized (messages) {
            messages.add( getStringParameter(parameters, "message") + " - " + getStringParameter(parameters, "@trigger") );
        }
        return Collections.emptyMap();
    }
}
