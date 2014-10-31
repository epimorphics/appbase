/******************************************************************
 * File:        ActionExecution.java
 * Created by:  Dave Reynolds
 * Created on:  16 Jun 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;

import com.epimorphics.appbase.core.TimerManager;
import com.epimorphics.appbase.tasks.impl.InternalAction;
import com.epimorphics.appbase.util.TimeStamp;
import com.epimorphics.json.JSFullWriter;
import com.epimorphics.json.JSONWritable;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.tasks.SimpleProgressMonitor;
import com.epimorphics.tasks.TaskState;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.NameUtils;

/**
 * Holds the state of the execution of an asynchronous action,
 * it may be still running or terminates.
 */
public class ActionExecution implements Runnable, JSONWritable {
    // probably should switch to using jackson for this but a lot of the JSON code is already dependent on Jena version
    protected static final String ACTION_KEY = "action";
    protected static final String INSTANCE_NAME_KEY = "iname";
    protected static final String PARAMETERS_KEY = "parameters";
    protected static final String START_TIME_KEY = "startTime";
    protected static final String FINISH_TIME_KEY = "finishTime";
    protected static final String MONITOR_KEY = "monitor";
    protected static final String DURATION_KEY = "duration";
    protected static final String ID_KEY = "id";
    protected static final String RESULT_KEY = "result";
    
    private final ActionManager actionManager;
    protected ActionInstance instance;
    protected long startTime;
    protected long finishTime = 0;
    protected ProgressMonitorReporter monitor;
    protected String id = TimeStamp.makeTimestamp();
    protected Future<?> future;
    protected JsonObject result;
    
    public ActionExecution(ActionManager actionManager, ActionInstance instance, ProgressMonitorReporter monitor) {
        this.actionManager = actionManager;
        this.monitor = monitor;
        this.instance = instance;
        instance.addConfig(ActionManager.ACTION_EXECUTION_PARAM, id);
    }
 
    public Action getAction() {
        return instance.getAction();
    }
    
    public ActionInstance getActionInstance() {
        return instance;
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
    
    public String getName() {
        return instance.getName();
    }
    
    public void setName(String name) {
        instance.setName(name);
    }
    
    public long getDuration() {
        if (finishTime > 0) {
            return finishTime - startTime;
        } else {
            return -1;
        }
    }
    
    public String getFormattedDuration() {
        long duration = getDuration();
        if (duration == -1) {
            return "Not finished";
        } else {
            return NameUtils.formatDuration(duration);
        }
    }
    
    public JsonObject getResult() {
        return result;
    }
    
    public JsonObject getParameters() {
        return instance.getCall();
    }
    
    @Override
    public void run() {
        this.actionManager.actionStartEvent(this, getParameters());
        startTime = System.currentTimeMillis();
        monitor.setState(TaskState.Running);
        startTimeout();
        result = instance.run(monitor);
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
            monitor.setState(TaskState.Terminated);
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
        monitor.reportError(message);
    }
    
    protected void runNext(Action next) {
        if (next != null) {
            next.run(JsonUtil.emptyObject(), new NestedProgressReporter(monitor));
        }
    }
    
    @Override
    public void writeTo(JSFullWriter out) {
        out.startObject();
        out.pair(ACTION_KEY, getAction().getName());
        out.pair(INSTANCE_NAME_KEY,  instance.getName());
        out.pair(ID_KEY, id);
        out.pair(PARAMETERS_KEY, getParameters());
        out.pair(DURATION_KEY, getDuration());
        out.pair(START_TIME_KEY, startTime);
        out.pair(FINISH_TIME_KEY, finishTime);
        if (monitor instanceof JSONWritable) {
            out.pair(MONITOR_KEY, (JSONWritable)monitor);
        } else {
            throw new EpiException("Monitor does not support JSON serialization");
        }
        if (result != null) {
            out.pair(RESULT_KEY, result);
        }
        out.finishObject();
    }
    
    /**
     * Reconstruct an ActionExecution from a JSON record.
     * Assumes that the record is correctly formated.
     */
    public static ActionExecution reload(ActionManager actionManager, InputStream in) {
        JsonObject jo = JSON.parse(in);
        String actionName = JsonUtil.getStringValue(jo, ACTION_KEY);
        Action action = actionManager.get( actionName );
        if (action == null) {
            action = new InternalAction(actionName);
        }
        JsonObject parameters = jo.get(PARAMETERS_KEY).getAsObject();
        SimpleProgressMonitor monitor = new SimpleProgressMonitor( jo.get(MONITOR_KEY).getAsObject() );
        ActionInstance ai = actionManager.makeInstance(action, parameters);
        String name = JsonUtil.getStringValue(jo, INSTANCE_NAME_KEY, actionName);
        ai.setName(name);
        ActionExecution ae = new ActionExecution(actionManager, ai, monitor);
        ae.startTime = JsonUtil.getLongValue(jo, START_TIME_KEY, -1);
        ae.finishTime = JsonUtil.getLongValue(jo, FINISH_TIME_KEY, -1);
        ae.id = JsonUtil.getStringValue(jo, ID_KEY);
        JsonValue result = jo.get(RESULT_KEY);
        if (result != null) {
            ae.result = result.getAsObject();
        }
        return ae;
    }
    
    /**
     * Reconstruct an ActionExecution from a JSON record.
     * Assumes that the record is correctly formated.
     */
    public static ActionExecution reload(ActionManager actionManager, File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            return reload(actionManager, in);
        } catch (Exception e) {
            ActionManager.log.error("Failed to load action execution " + file, e);
            return null;
        } finally {
            in.close();
        }
    }
}