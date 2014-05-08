/******************************************************************
 * File:        JavaAction.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * Action that calls a java class with some additional configuration 
 * parameters bound in.
 */
public class JavaAction extends BaseAction implements Action {
    protected Action baseAction;

    public void setAction(String classname) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        baseAction = (Action)Class.forName(classname).newInstance();
        mergeBaseConfiguration(baseAction);
    }
    
    @Override
    public JsonObject doRun(JsonObject parameters, ProgressMonitorReporter monitor) {
        return baseAction.run(JsonUtil.merge(configuration, parameters), monitor);
    }

}
