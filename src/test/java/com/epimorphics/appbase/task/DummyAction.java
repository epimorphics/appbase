/******************************************************************
 * File:        DummyAction.java
 * Created by:  Dave Reynolds
 * Created on:  19 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import java.util.Collections;
import java.util.Map;


import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.tasks.ProgressMonitorReporter;
import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.NAME_KEY;

/**
 * Used for testing the action sub-system
 */
public class DummyAction extends BaseAction implements Action {

    @Override
    public int getTimeout() {
        return 50;
    }

    @Override
    protected Map<String, Object> doRun(Map<String, Object> parameters,
            ProgressMonitorReporter monitor) {
        monitor.report( getStringParameter(parameters, NAME_KEY) + " started");
        String msg = getStringParameter(parameters, "message", "no message");
        monitor.report( msg );
        for (int i = 1; i < getIntParameter(parameters, "count", 1); i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return Collections.emptyMap();
            }
            monitor.report("" + parameters.get("message"));
        }
        monitor.report(getStringParameter(parameters, NAME_KEY) + " finished");
        return Collections.singletonMap("result", (Object)("Message: " + msg));
    }

}
