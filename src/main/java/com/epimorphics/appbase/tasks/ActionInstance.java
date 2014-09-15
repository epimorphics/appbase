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
        JsonObject thiscall = JsonUtil.merge(call, parameters);
        JsonObject result = safeRun(baseAction, thiscall, monitor);
        
        String aeid = JsonUtil.getStringValue(thiscall, ActionManager.ACTION_EXECUTION_PARAM, null);
        if (aeid != null) {
            // Merge rather than put to avoid overwriting the EMPTY_OBJECT
            result = JsonUtil.makeJson(result, ActionManager.ACTION_EXECUTION_PARAM, aeid);
        }
        
        if (monitor.succeeded()) {
            synchronized (this) {
                for (Action a : onSuccessList) {
                    safeRun(a, result, monitor);
                }
            }
        } else {
            synchronized (this) {
                for (Action a : onErrorList) {
                    safeRun(a, result, monitor);
                }
            }
        }
        if (monitor.succeeded()) {
            if (baseAction.getOnSuccess() != null) {
                safeRun(baseAction.getOnSuccess(), result, monitor);
            }
        } else {
            if (baseAction.getOnError() != null) {
                safeRun(baseAction.getOnError(), result, monitor);
            }
        }
        if (monitor.getState() != TaskState.Terminated) {
            monitor.setState(TaskState.Terminated);
        }
        return result;
    }
    
    private JsonObject safeRun(Action a, JsonObject params, ProgressMonitorReporter monitor) {
        try {
            return a.run(params, monitor);
        } catch (Exception e) {
            ActionManager.log.error("Exception during action execution", e);
            monitor.reportError("Exception during execution: " + e);
            return JsonUtil.emptyObject();
        }
    }
    
    /**
     * Set the instance running as a separate background action
     * with no additional parameters
     */
    public ActionExecution start() {
        return am.runInstance(this);
    }
    
    /**
     * Run the instance
     */
    public JsonObject run(ProgressMonitorReporter monitor) {
        return run(JsonUtil.emptyObject(), monitor);
    }
    
    /**
     * Run the instance from within an enclosing action
     */
    public JsonObject runNested(ProgressMonitorReporter monitor) {
        NestedProgressReporter nmon = new NestedProgressReporter(monitor);
        JsonObject result = run(JsonUtil.emptyObject(), nmon);
        if (! nmon.succeeded() ) {
            monitor.setFailed();
        }
        return result;
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
        runNested(monitor);
        return monitor.succeeded();
    }
     
}
