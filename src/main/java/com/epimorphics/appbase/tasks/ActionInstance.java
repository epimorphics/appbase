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

import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.tasks.SimpleProgressMonitor;
import com.epimorphics.tasks.TaskState;
import com.epimorphics.util.EpiException;

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
public class ActionInstance implements Action {
    protected Action baseAction;
    protected JsonObject call = new JsonObject();
    protected List<Action> onSuccessList = new ArrayList<>();
    protected List<Action> onErrorList = new ArrayList<>();
    protected ActionManager am;
    protected int timeout;
    protected String name;

    protected ActionInstance(Action base, JsonObject parameters, ActionManager am) {
        JsonUtil.mergeInto(call, parameters);
        baseAction = base;
        this.am = am;
        baseAction.resolve(am);
        timeout = base.getTimeout();
        name = base.getName();
    }
    
    public Action getAction() {
        return baseAction;
    }
    
    public JsonObject getCall() {
        return call;
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
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
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
    
    /**
     * Add an additional action to run if this action succeeds
     */
    public void addOnSuccess(Action next) {
        onSuccessList.add( next );
    }
    
    /**
     * Add an additional action to run if this action fails
     */
    public synchronized void addOnError(Action next) {
        onErrorList.add( next );
    }
    
    /**
     * Add an additional action to run whether or not this action succeeds
     */
    public synchronized void addAndThen(Action next) {
        onSuccessList.add( next );
        onErrorList.add( next );
    }
    
    public void setConfig( Object...args ) {
        call = JsonUtil.makeJson(call, args);
    }
    
    public void addConfig( JsonObject conf ) {
        JsonUtil.mergeInto(call, conf);
    }
    
    public void addConfig( Object...args ) {
        addConfig( JsonUtil.makeJson(args) );
    }
    
    @Override
    public JsonObject run(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        JsonUtil.mergeInto(call, parameters);
        JsonObject result = null;
        try {
            result = baseAction.run(call, monitor);
            if (monitor.getState() != TaskState.Terminated) {
                monitor.setState(TaskState.Terminated);
            }
        } catch (Exception e) {
            ActionManager.log.error("Exception during action execution", e);
            monitor.report("Exception during execution: " + e);
            monitor.setFailed();
        }
        if (monitor.succeeded()) {
            synchronized (this) {
                for (Action a : onSuccessList) {
                    a.run(result, monitor);
                }
            }
        } else {
            synchronized (this) {
                for (Action a : onErrorList) {
                    a.run(result, monitor);
                }
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
        return run(JsonUtil.EMPTY_OBJECT, monitor);
    }
    
    /**
     * Run the instance in the top level thread.
     * Throw an exception if the action fails.
     * Return the action results.
     * The progress messages are lost.
     */
    public JsonObject runInline() {
        SimpleProgressMonitor monitor = new SimpleProgressMonitor();
        JsonObject result = run(monitor);
        if (monitor.succeeded()) {
            return result;
        } else {
            List<ProgressMessage> messages = monitor.getMessages();
            if (messages.isEmpty()) {
                throw new EpiException("Action failed, no further information");
            } else {
                throw new EpiException("Action failed: " + messages.get( messages.size() - 1 ).getMessage() );
            }
        }
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
