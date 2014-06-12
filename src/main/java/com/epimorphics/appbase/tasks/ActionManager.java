/******************************************************************
 * File:        ActionManager.java
 * Created by:  Dave Reynolds
 * Created on:  18 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jena.atlas.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.Shutdown;
import com.epimorphics.appbase.core.TimerManager;
import com.epimorphics.appbase.monitor.ConfigMonitor;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.tasks.SimpleProgressMonitor;
import com.epimorphics.tasks.TaskState;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;

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
public class ActionManager extends ConfigMonitor<Action> implements Shutdown {
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
    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
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
    
    @Override
    public void shutdown() {
        if (actionLog != null) {
            try {
                actionLog.close();
            } catch (IOException e) {
                // ignore, we are shutting down anyway
            }
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
        return executionIndex.get(id);
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
        ActionExecution ae = new ActionExecution(action, parameters, monitor);
        recordExecution(ae);
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
    protected List<ActionExecution> actionStartEvent(Action action, JsonObject parameters) {
        return fireEvent("action:" + action.getName() + ":started", parameters);
    }
    
    /**
     * Send event signalling the end of an action
     */
    protected List<ActionExecution> actionEndEvent(ActionExecution ae, JsonObject result) {
        String success = ae.getMonitor().succeeded() ? "succeeded" : "failed";
        if (traceDir != null) {
            try {
                FileWriter fw = new FileWriter( new File(traceDir, "trace-" + ae.getId()) );
                for (ProgressMessage message : ae.getMonitor().getMessages()) {
                    fw.write(message.toString() + "\n");
                }
                fw.write( String.format("** Action %s %s in %d milliseconds\n", ae.getAction().getName(), success, ae.getDuration()) );
                fw.close();
            } catch (IOException e) {
                log.error("Failed to write action trace", e);
            }
        }
        String msg = String.format("action:%s:finished %s %d", 
                ae.getAction().getName(), success, ae.getDuration());
        return fireEvent(msg, result);
    }
    
    /**
     * Log an event
     */
    protected void logEvent(String event, JsonObject parameters) {
        String msg = event + " " + parameters;
        log.info(msg);
        if (actionLog != null) {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format( new Date() );
            try {
                // TODO Use Directory watcher to make this more efficient?
                if (!logF.exists()) {  
                    // log file deleted while we were running?
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
    
    /**
     * Holds the state of the execution of an asynchronous action,
     * it may be still running or terminates.
     */
    public class ActionExecution implements Runnable {
        protected Action action;
        protected JsonObject parameters;
        protected long startTime;
        protected long finishTime = 0;
        protected ProgressMonitorReporter monitor;
        protected String id = UUID.randomUUID().toString();
        protected Future<?> future;
        protected JsonObject result;
        
        public ActionExecution(Action action, JsonObject parameters) {
            this(action, parameters, new SimpleProgressMonitor());
        }
        
        public ActionExecution(Action action, JsonObject parameters, ProgressMonitorReporter monitor) {
            this.parameters = JsonUtil.makeJson(parameters, ACTION_EXECUTION_PARAM, id);
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
            actionStartEvent(action, parameters);
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
                log.error("Exception during action execution " + id, e);
                condMarkTerminated("Exception: " + e);
            }
            finishTime = System.currentTimeMillis();
            recordEndOfExecution(this);
            if (monitor.getState() != TaskState.Terminated) {
                condMarkTerminated("Thread died before completion, cause unknown");
            }
            actionEndEvent(this, result);
        }
        
        private Future<?> start() {
            future = executor.submit(this);
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
            actionEndEvent(this, result);
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
    
}
