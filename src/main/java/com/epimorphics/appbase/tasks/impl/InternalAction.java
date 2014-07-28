/******************************************************************
 * File:        InternalAction.java
 * Created by:  Dave Reynolds
 * Created on:  28 Jul 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionJsonFactorylet;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * Dummy action used to represent a programmatic action when
 * displaying logs retrieved from trace files.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class InternalAction extends BaseAction implements Action {

    public InternalAction(String name) {
        configuration.put(ActionJsonFactorylet.NAME_KEY, name);
    }
    
    @Override
    protected JsonObject doRun(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        return null;
    }

}
