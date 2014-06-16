/******************************************************************
 * File:        ActionExecution.java
 * Created by:  Dave Reynolds
 * Created on:  16 Jun 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.core.TimerManager;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.tasks.SimpleProgressMonitor;
import com.epimorphics.tasks.TaskState;

/**
 * Holds the state of the execution of an asynchronous action,
 * it may be still running or terminates.
 */
public class ActionExecution implements Runnable {
    private final ActionManager actionManager;
    protected Action action;
    protected JsonObject parameters;
    protected long startTime;
    protected long finishTime = 0;
    protected ProgressMonitorReporter monitor;
    protected String id = UUID.randomUUID().toString();
    protected Future<?> future;
    protected JsonObject result;
    
    public ActionExecution(ActionManager actionManager, Action action, JsonObject parameters) {
        this(actionManager, action, parameters, new SimpleProgressMonitor());
    }
    
    public ActionExecution(ActionManager actionManager, Action action, JsonObject parameters, ProgressMonitorReporter monitor) {
        this.actionManager = actionManager;
        this.parameters = JsonUtil.makeJson(parameters, ActionManager.ACTION_EXECUTION_PARAM, id);
        this.monitor = monitor;
        this.action = action;
    }
    
    public Action getAction() {
        return action;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public ProgressMonitorReporter getMonitor() {
        return monitor;
    }

    public String getId() {
        return id;
    }
    
    public long getDuration() {
        if (finishTime > 0) {
            return finishTime - startTime;
        } else {
            return -1;
        }
    }
    
    public JsonObject getResult() {
        return result;
    }

    
    @Override
    public void run() {
        this.actionManager.actionStartEvent(action, parameters);
        startTime = System.currentTimeMillis();
        monitor.setState(TaskState.Running);
        startTimeout();
        try {
            result = action.run(parameters, monitor);
            if (monitor.getState() != TaskState.Terminated) {
                monitor.setSucceeded();
            }
            if (monitor.succeeded()) {
                runNext( action.getOnSuccess() );
            } else {
                runNext( action.getOnError() );
            }
        } catch (Throwable e) {
            ActionManager.log.error("Exception during action execution " + id, e);
            condMarkTerminated("Exception: " + e);
        }
        finishTime = System.currentTimeMillis();
        this.actionManager.recordEndOfExecution(this);
        if (monitor.getState() != TaskState.Terminated) {
            condMarkTerminated("Thread died before completion, cause unknown");
        }
        this.actionManager.actionEndEvent(this, result);
    }
    
    Future<?> start() {
        future = this.actionManager.executor.submit(this);
        return future;
    }

    private void startTimeout() {
        int timeout = getAction().getTimeout();
        if (timeout != -1) {
            TimerManager.get().schedule(new Runnable() {
                @Override  public void run() { 
                    timeout();
                }
            }, timeout, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Cancel the execution, recording the given message on the progress monitor
     */
    public void cancel(String message) {
        if ( ! future.isDone() ) {
            condMarkTerminated(message);
            future.cancel(true);
        }
    }
    
    public void timeout() {
        cancel("Terminated due to timeout");
        this.actionManager.actionEndEvent(this, result);
    }
    
    public void waitForCompletion() {
        try {
            future.get();
        } catch(Exception e) {
            // Ignore interruption exception
        }
    }
    
    protected void condMarkTerminated(String message) {
        ProgressMonitorReporter monitor = getMonitor();
        if (monitor.getState() != TaskState.Terminated) {
            monitor.report(message);
            monitor.setFailed();
        }
        runNext( action.getOnError() );
    }
    
    protected void runNext(Action next) {
        if (next != null) {
            next.run(JsonUtil.EMPTY_OBJECT, new NestedProgressReporter(monitor));
        }
    }
    
}