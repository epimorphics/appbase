/******************************************************************
 * File:        CreateErrorAction.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;

/**
 * Test action which throws an exception to test error handling
 */
public class CreateErrorAction extends BaseAction {

    @Override
    protected JsonObject doRun(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        throw new EpiException("Forcing error from CreateErrorAction");
    }

}
