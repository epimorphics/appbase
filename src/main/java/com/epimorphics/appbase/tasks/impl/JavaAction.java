/******************************************************************
 * File:        JavaAction.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import java.util.HashMap;
import java.util.Map;

import com.epimorphics.appbase.tasks.Action;
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
    public void doRun(Map<String, Object> parameters, ProgressMonitorReporter monitor) {
        // Could have a chained map implementation a bit like dclib's BindingEnv but not really necessary
        Map<String, Object> call = new HashMap<>( configuration );
        call.putAll(parameters);
        baseAction.run(call, monitor);
    }

}
