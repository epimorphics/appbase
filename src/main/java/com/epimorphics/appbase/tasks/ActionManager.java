/******************************************************************
 * File:        ActionManager.java
 * Created by:  Dave Reynolds
 * Created on:  18 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jena.atlas.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Shutdown;
import com.epimorphics.appbase.core.Startup;
import com.epimorphics.appbase.monitor.ConfigMonitor;
import com.epimorphics.appbase.tasks.ProcessingHook.Event;
import com.epimorphics.json.JSFullWriter;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.tasks.SimpleProgressMonitor;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;
import org.slf4j.MDC;

/**
 * Controller which tracks available actions, executes actions
 * and tracks ongoing and recently completed executions.
 * Monitors a configurable directory - see ConfigMonitor for configuration options for that.
 * <p>Additional configuration options:</p>
 * <ul>
 * <li>maxHistory - how many completed ActionExecutions to retain in memory</li>
 * <li>factories - comma-separated list of javaclass names for ActionFactory factorylets to use for parsing configuration files</li>
 * <li>logDirectory - file name for a separate log of all actions, actions will still be included in the webapp log</li>
 * </ul>
 */
public class ActionManager extends ConfigMonitor<Action> implements Shutdown, Startup {
    static Logger log = LoggerFactory.getLogger(ActionManager.class);
    
    public static final String ACTION_EXECUTION_PARAM = "actionExecutionID";
    
    protected static final int DEFAULT_HISTORY_SIZE = 500;
    private static final int MAX_THREADS = 20;
    private static final int CORE_THREADS = 10;
    private static final int QUEUE_DEPTH = 10;
    private static final int KEEPALIVE = 1000;
    
    // Table of available actions provided by super class
    
    protected Set<ActionExecution> currentExecutions = new HashSet<>();
    protected Map<String, ActionExecution> executionIndex = new HashMap<String, ActionExecution>();
    protected Deque<ActionExecution> executionHistory = new ArrayDeque<>(DEFAULT_HISTORY_SIZE);
    protected Set<Action> triggerableActions = new HashSet<>();
    protected Map<Event, List<ProcessingHook>> installedHooks = new HashMap<>();

    protected ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, KEEPALIVE, TimeUnit.MILLISECONDS, 
            new ArrayBlockingQueue<Runnable>(QUEUE_DEPTH));
    
    // Configuration options beyond base ConfigMonitor
    protected int maxHistory = DEFAULT_HISTORY_SIZE;
    protected File logF;
    protected FileWriter actionLog;
    protected File traceDir = null;
    protected File scriptDir = null;

    /**
     * Configure the maximum number of past executions which are retain for review, default is 500
     */
    public void setMaxHistory(long maxHistory) {
        this.maxHistory = (int) maxHistory;
    }

    /**
     * Configure a log file to which events (e.g. action start/end) will be logged
     */
    public void setLogFile(String logf) {
        try {
            logF = asFile(logf);
            FileUtil.ensureDir( logF.getParentFile().getPath() );
            actionLog = new FileWriter( logF, true);
        } catch (IOException e) {
            throw new EpiException("Problem opening action log file: " + logf, e);
        }
    }
    
    /**
     * Configure a directory to which traces of action of executions will be recorded
     */
    public void setTraceDir(String dir) {
        traceDir = asFile(dir);
        FileUtil.ensureDir(traceDir.getPath());
        if (! traceDir.isDirectory() || ! traceDir.canWrite() ) {
            throw new EpiException("Problem accessing trace directory: " + dir);
        }
    }
    
    public String getTraceDir() {
        return traceDir == null ? null : traceDir.getPath();
    }
    
    /**
     * Configure a directory which will hold shell scripts usable as actions.
     */
    public void setScriptDir(String dir) {
        scriptDir = asFile(dir);
    }
    
    public String getScriptDir() {
        return scriptDir.getPath();
    }
    
    /**
     * Install a processing hook that will be run at certain
     * stages in the action execution lifecycle. 
     */
    public void installHook(ProcessingHook hook) {
        Event event = hook.runOn();
        List<ProcessingHook> hooks = installedHooks.get(event);
        if (hooks == null) {
            hooks = new ArrayList<>();
            installedHooks.put(event, hooks);
        }
        hooks.add(hook);
    }
    
    protected void runHooks(ProcessingHook.Event event, ActionExecution execution) {
        List<ProcessingHook> hooks = installedHooks.get(event);
        if (hooks != null) {
            for (ProcessingHook hook : hooks) {
                hook.run(execution);
            }
        }
    }
    
    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e1) {
            // No action 
        }
        if (actionLog != null) {
            try {
                actionLog.close();
            } catch (IOException e) {
                // ignore, we are shutting down anyway
            }
        }
    }
    
    @Override
    public void startup(App app) {
        super.startup(app);
        if (traceDir != null) {
            loadTraceHistory();
        }
    }
    
    private void loadTraceHistory() {
        String[] tracesFiles = traceDir.list();
        Arrays.sort(tracesFiles);
        int start = Math.max(0, tracesFiles.length - maxHistory);
        try {
            for (int i = start; i < tracesFiles.length; i++) {
                ActionExecution ae = ActionExecution.reload(this, new File(traceDir, tracesFiles[i]));
                if (ae != null)
                    executionHistory.add(ae);
            }   
            log.info("Loaded " + executionHistory.size() + " historical action traces");
        } catch (IOException e) {
            log.error("Failed to reload historical action traces", e);
        }
    }
    
    // Trap any triggerable actions
    @Override
    protected void doAddEntry(Action entry) {
        super.doAddEntry(entry);
        if (entry.getTrigger() != null) {
            triggerableActions.add(entry);
        }
    }
    
    @Override
    protected void doRemoveEntry(Action entry) {
        super.doRemoveEntry(entry);
        if (entry.getTrigger() != null) {
            triggerableActions.remove(entry);
        }
    }
    
    /**
     * Set ActionFactory entries
     * @param factories commas separate list of class names for factorylets
     */
    public void setFactories(String factories) {
        for (String factoryName : factories.split(",")) {
            try {
                ActionFactory.Factorylet factory = (ActionFactory.Factorylet) Class.forName(factoryName).newInstance();
                ActionFactory.register(factory);
            } catch (Exception e) {
                throw new EpiException("Problem instantiating action factory", e);
            }
        }
    }
    
    @Override
    protected Collection<Action> configure(File file) {
        try {
            return ActionFactory.configure(file);
        } catch (Throwable e) {
            log.error("Problem loading config file: " + file, e);
            return Collections.emptyList();
        }
    }

    protected synchronized void recordExecution(ActionExecution ae) {
        currentExecutions.add(ae);
        executionIndex.put(ae.getId(), ae);
        executionHistory.addLast(ae);
        if (executionHistory.size() > maxHistory) {
            ActionExecution discard = executionHistory.removeFirst();
            executionIndex.remove( discard.getId() );
        }
    }
    
    protected synchronized void recordEndOfExecution(ActionExecution ae) {
        currentExecutions.remove(ae);
    }

    /**
     * Return an identified execution, may no longer be active.
     */
    public synchronized ActionExecution getExecution(String id) {
        ActionExecution ae = executionIndex.get(id);
        if (ae == null && traceDir != null) {
            try {
                ae = ActionExecution.reload(this, getTraceFile(id));
            } catch (IOException e) {
                log.error("Failed to retrieve persisted action execution for: " + id);
            }
        }
        return ae;
    }

    /**
     * Return the executor which runs all actions, can be used
     * by actions to schedule parallel tasks
     */
    public ExecutorService getExecutor() {
        return executor;
    }
    
    /**
     * Return all executions that are still active.
     */
    public synchronized Collection<ActionExecution> listActiveExecutions() {
        return new ArrayList<>( currentExecutions );
    }
    
    /**
     * Return the last N executions, whether or not completed
     */
    public synchronized List<ActionExecution> listRecentExecutions(int n) {
        n = Math.max(n, executionHistory.size());
        List<ActionExecution> results = new ArrayList<>(n);
        Iterator<ActionExecution> it = executionHistory.descendingIterator();
        for (int i = 0; i < n && it.hasNext(); i++) {
            results.add( it.next() );
        }
        return results;
    }
    
    /**
     * Start the action running as a background thread
     * @param action the action
     * @param parameters configuration and runtime parameters
     * @return a future which can be used to wait for the action to complete or timeout
     */
    public ActionExecution runAction(Action action, JsonObject parameters) {
        return runAction(action, parameters, new SimpleProgressMonitor());
    }
    
    /**
     * Start the action running as a background thread
     * @param action the action
     * @param parameters configuration and runtime parameters
     * @param an external progress monitor to use
     * @return a future which can be used to wait for the action to complete or timeout
     */
    public ActionExecution runAction(Action action, JsonObject parameters, ProgressMonitorReporter monitor) {
        action.resolve(this);
        return runInstance( makeInstance(action, parameters), monitor );
    }
    
    /**
     * Start the prepared action instance funning as a background thread
     */
    public ActionExecution runInstance(ActionInstance instance) {
        return runInstance(instance, new SimpleProgressMonitor());
    }
    
    /**
     * Start the prepared action instance funning as a background thread
     */
    public ActionExecution runInstance(ActionInstance instance, ProgressMonitorReporter monitor) {
        ActionExecution ae = new ActionExecution(this, instance, monitor);
        recordExecution(ae);
        runHooks(Event.Start, ae);
        ae.start();
        return ae;
    }

    /**
     * Start the action running as a background thread
     * @param actionName the name of an action registered with this manager
     * @param parameters configuration and runtime parameters
     * @return a future which can be used to wait for the action to complete or timeout
     */
    public ActionExecution runAction(String actionName, JsonObject parameters) {
        return runAction( get(actionName),  parameters);
    }
    
    /**
     * Create an instance of an action that is ready to run.
     */
    public ActionInstance makeInstance(Action action, Object...args) {
        return new ActionInstance(action, JsonUtil.makeJson(args), this);
    }
    
    /**
     * Create an instance of an action that is ready to run.
     */
    public ActionInstance makeInstance(Action action, JsonObject args) {
        return new ActionInstance(action, args, this);
    }
    
    /**
     * Create an instance of an action that is ready to run.
     * Returns null if the action can't be found
     */
    public ActionInstance makeInstance(String actionName, Object...args) {
        Action action = get(actionName);
        if (action == null){
            return null;
        }
        return new ActionInstance(action, JsonUtil.makeJson(args), this);
    }
    
    /**
     * Send an event which will trigger any matching actions.
     */
    public List<ActionExecution> fireEvent(String event, JsonObject parameters) {
        logEvent(event, parameters);
        List<ActionExecution> executions = new ArrayList<>();
        for (Action action : triggerableActions) {
            if (action.getTrigger().matches(event, parameters)) {
                JsonObject callParams = JsonUtil.makeJson(parameters, ActionTrigger.TRIGGER_KEY, event);
                executions.add( runAction(action, callParams) );
            }
        }
        return executions;
    }
    
    /**
     * Send event signalling the start of an action
     */
    protected List<ActionExecution> actionStartEvent(ActionExecution ae, JsonObject parameters) {
        String msg = String.format("action:%s:%s:started", ae.getId(), ae.getAction().getName());
        return fireEvent(msg, parameters);
    }
    
    /**
     * Send event signalling the end of an action
     */
    protected List<ActionExecution> actionEndEvent(ActionExecution ae, JsonObject result) {
        runHooks(Event.Complete, ae);
        if ( ! ae.getMonitor().succeeded() ) {
            runHooks(Event.Error, ae);
        }
        recordTrace(ae);
        String success = ae.getMonitor().succeeded() ? "succeeded" : "failed";
        String msg = String.format("action:%s:%s:finished %s %d", 
                ae.getId(), ae.getAction().getName(), success, ae.getDuration());
        return fireEvent(msg, result);
    }
    
    /**
     * Record a trace of the entire execution history
     */
    protected void recordTrace(ActionExecution ae) {
        if (traceDir != null) {
            try {
                File traceFile = getTraceFile( ae.getId() );
                OutputStream outs = new BufferedOutputStream( new FileOutputStream( traceFile ) );
                JSFullWriter jsout = new JSFullWriter(outs);
                jsout.startOutput();
                ae.writeTo( jsout );
                jsout.finishOutput();
                outs.close();
            } catch (IOException e) {
                log.error("Failed to write action trace", e);
            }
        }
    }

    /**
     * Return the file name of the trace file to use to record an execution
     * or null tracing is not enabled
     */
    protected File getTraceFile(String id) {
        if (traceDir != null) {
            return new File(traceDir, "trace-" + id + ".json");
        } else {
            return null;
        }
    }
    /**
     * Log an event
     */
    protected void logEvent(String event, JsonObject parameters) {
        if (parameters.hasKey(ACTION_EXECUTION_PARAM) && parameters.get(ACTION_EXECUTION_PARAM).isString()) {
            MDC.put(ACTION_EXECUTION_PARAM, parameters.get(ACTION_EXECUTION_PARAM).getAsString().value());
        }
        StringBuffer msg = new StringBuffer();
        msg.append(event);
        for (String key : parameters.keys()) {
            if (key != ACTION_EXECUTION_PARAM) {
                msg.append(" " + key + "=" + parameters.get(key));
            }
        }
        log.info(msg.toString());
        if (actionLog != null) {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format( new Date() );
            try {
                // TODO Use Directory watcher to make this more efficient?
                if (!logF.exists()) {  
                    // log file deleted while we were running?
                    if (actionLog != null) {
                        actionLog.close();
                    }
                    actionLog = new FileWriter( logF, true);
                }
                actionLog.write(dateStr + " " + msg + "\n");
                actionLog.flush();
            } catch (IOException e) {
                // TODO try reopening?
                log.error("Problem writing to action log", e);
            }
        }
    }

}
