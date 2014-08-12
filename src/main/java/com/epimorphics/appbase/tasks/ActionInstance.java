/******************************************************************
 * File:        ActionInstance.java
 * Created by:  Dave Reynolds
 * Created on:  12 Aug 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * A ready-to-run action. Combines some base action with optional
 * additional parameter settings and follow on actions. The call parameters, 
 * follow on actions and timeout for the instance can be modified before 
 * running it without affecting the base action.
 * <p>
 * Actions don't have to be wrapped into ActionInstances to use them. This is an
 * experimental extension.
 * </p>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ActionInstance extends BaseAction implements Action {
    protected Action baseAction;
    protected List<Action> onSuccessList = new ArrayList<>();
    protected List<Action> onErrorList = new ArrayList<>();
    protected ActionManager am;
    protected int timeout;

    protected ActionInstance(Action base, JsonObject parameters, ActionManager am) {
        JsonUtil.mergeInto(configuration, parameters);
        baseAction = base;
        this.am = am;
        baseAction.resolve(am);
        timeout = base.getTimeout();
    }
    
    @Override
    public void resolve(ActionManager am) {
        // already resolved at creation time
    }

    @Override
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    @Override
    public ActionTrigger getTrigger() {
        return baseAction.getTrigger();
    }
    
    @Override
    /**
     * Returns any underlying onError action, not any dynamically
     * added ones.
     */
    public Action getOnError() {
        return baseAction.getOnError();
    }

    /**
     * Returns any underlying onSuccess action, not any dynamically
     * added ones.
     */
    @Override
    public Action getOnSuccess() {
        return baseAction.getOnSuccess();
    }
    
    @Override
    public void setOnError(Action onError) {
        addOnError(onError);
    }

    @Override
    public void setOnSuccess(Action onSuccess) {
        addOnSuccess(onSuccess);
    }
    
    /**
     * Add an additional action to run if this action succeeds
     */
    public void addOnSuccess(Action next) {
        onSuccessList.add( next );
    }
    
    /**
     * Add an additional action to run if this action fails
     */
    public void addOnError(Action next) {
        onErrorList.add( next );
    }
    
    /**
     * Add an additional action to run whether or not this action succeeds
     */
    public void addAndThen(Action next) {
        onSuccessList.add( next );
        onErrorList.add( next );
    }
    
    public void setConfig( Object...args ) {
        configuration = JsonUtil.makeJson(configuration, args);
    }

    @Override
    protected JsonObject doRun(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        JsonObject call = JsonUtil.merge(configuration, parameters);
        JsonObject result = baseAction.run(call, monitor);
        if (monitor.succeeded()) {
            for (Action a : onSuccessList) {
                a.run(result, monitor);
            }
        } else {
            for (Action a : onErrorList) {
                a.run(result, monitor);
            }
        }
        if (monitor.succeeded()) {
            if (baseAction.getOnSuccess() != null) {
                baseAction.getOnSuccess().run(result, monitor);
            }
        } else {
            if (baseAction.getOnError() != null) {
                baseAction.getOnError().run(result, monitor);
            }
        }
        return result;
    }
    
    
    /**
     * Set the instance running as a separate background action
     * with no additional parameters
     */
    public ActionExecution start() {
        return am.runAction(this, JsonUtil.EMPTY_OBJECT);
    }
    
    /**
     * Run the instance from within an enclosing action
     */
    public JsonObject run(ProgressMonitorReporter monitor) {
        return doRun(JsonUtil.EMPTY_OBJECT, monitor);
    }
    
    
    /**
     * Run the closure from within an enclosing action,
     * returning true if the call succeeded 
     */
    public boolean runAndReport(ProgressMonitorReporter monitor) {
        run(monitor);
        return monitor.succeeded();
    }
}
