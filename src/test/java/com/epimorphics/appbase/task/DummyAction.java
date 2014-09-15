/******************************************************************
 * File:        DummyAction.java
 * Created by:  Dave Reynolds
 * Created on:  19 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.NAME_KEY;
import static com.epimorphics.json.JsonUtil.getIntValue;
import static com.epimorphics.json.JsonUtil.getStringValue;
import static com.epimorphics.json.JsonUtil.makeJson;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * Used for testing the action sub-system
 */
public class DummyAction extends BaseAction implements Action {

    @Override
    public int getTimeout() {
        return 50;
    }

    @Override
    protected JsonObject doRun(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        monitor.report( getStringValue(parameters, NAME_KEY, null) + " started");
        String msg = getStringValue(parameters, "message", "no message");
        monitor.report( msg );
        for (int i = 1; i < getIntValue(parameters, "count", 1); i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return JsonUtil.emptyObject();
            }
            monitor.report(msg);
        }
        monitor.report(getStringValue(parameters, NAME_KEY, null) + " finished");
        return makeJson("result", "Message: " + msg);
    }

}
