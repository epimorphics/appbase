/******************************************************************
 * File:        Closure.java
 * Created by:  Dave Reynolds
 * Created on:  9 Aug 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.impl.WrappedAction;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * A pre-prepared call to some action. Comprises the action (fully resolved),
 * call parameters and an optional on-completion step. Can be launched as a new
 * action or invoked inline as part of a surrounding action.
 */
public class Closure {
    protected Action action;
    protected ActionManager am;
    protected JsonObject call = new JsonObject();
    
    public Closure(Action action, ActionManager am) {
        this.action = action;
        this.am = am;
        action.resolve(am);
    }
    
    public Closure(String actionName, ActionManager am) {
        this( am.get(actionName), am);
    }
    
    public void setCall(JsonObject parameters) {
        call = JsonUtil.makeJson(parameters);
    }
    
    public void setParameter(String key, Object value) {
        call.put(key, JsonUtil.asJson(value));
    }
    
    public void setParameters( Object...args ) {
        call = JsonUtil.makeJson(call, args);
    }
    
    public void setAndThen(Action andThen) {
        WrappedAction wrap = new WrappedAction(action);
        wrap.setOnSuccess(andThen);
        action = wrap;
    }
    
    /**
     * Set the closure running as a separate background action
     */
    public ActionExecution run() {
        return am.runAction(action, call);
    }
    
    /**
     * Run the closure from within an enclosing action
     */
    public JsonObject call(ProgressMonitorReporter monitor) {
        JsonObject result = action.run(call, monitor);
        if (monitor.succeeded() && action.getOnSuccess() != null) {
            action.getOnSuccess().run(result, monitor);
        }
        if ( ! monitor.succeeded() && action.getOnError() != null) {
            action.getOnError().run(result, monitor);
        }
        return result;
    }
    
    
    /**
     * Run the closure from within an enclosing action,
     * returning true if the call succeeded 
     */
    public boolean callAndReport(ProgressMonitorReporter monitor) {
        action.run(call, monitor);
        return monitor.succeeded();
    }
}
