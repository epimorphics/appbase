/******************************************************************
 * File:        WrappedAction.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.BASE_KEY;

import java.util.HashMap;
import java.util.Map;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;

/**
 * An action that calls an existing action with some configuration parameters overridden
 */
public class WrappedAction extends BaseAction implements Action {
    protected Action baseAction;

    public Action getAction() {
        if (baseAction == null) {
            String actionName = getStringConfig(BASE_KEY , null);
            // Assumes single App with single ActionManager - TODO can we improve on this?
            ActionManager am = AppConfig.getApp().getA(ActionManager.class);
            baseAction = am.get(actionName);
            if (baseAction == null) {
                throw new EpiException("Can't find the action named: " + actionName + ", which is base for action " + getName());
            }
            mergeBaseConfiguration(baseAction);
        }
        return baseAction;
    }
    
    @Override
    public void doRun(Map<String, Object> parameters, ProgressMonitorReporter monitor) {
        Map<String, Object> call = new HashMap<>( configuration );
        call.putAll(parameters);
        getAction().run(call, monitor);
    }

}
