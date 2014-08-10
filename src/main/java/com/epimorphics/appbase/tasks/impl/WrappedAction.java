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

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * An action that calls an existing action with some configuration parameters overridden
 */
public class WrappedAction extends BaseAction implements Action {
    protected Action baseAction;
    
    public WrappedAction() {
    }
    
    public WrappedAction(Action baseAction) {
        this.baseAction = baseAction;
    }

    @Override
    public void resolve(ActionManager am) {
        super.resolve(am);
        if ( baseAction == null) {
            String actionName = getStringConfig(BASE_KEY , null);
            baseAction = resolveAction(am, actionName);
        }
        if (baseAction != null) {
            mergeBaseConfiguration(baseAction);
        }
    }
    
    @Override
    public JsonObject doRun(JsonObject parameters, ProgressMonitorReporter monitor) {
        if (baseAction == null) {
            monitor.report("Could not find base action: " + getStringConfig(BASE_KEY , null));
            monitor.setFailed();
            return JsonUtil.EMPTY_OBJECT;
        } else {
            return baseAction.run(JsonUtil.merge(configuration, parameters), monitor);
            
        }
    }

}
