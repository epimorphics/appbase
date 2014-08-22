/******************************************************************
 * File:        NestedProgressReporter.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.util.List;

import com.epimorphics.json.JSONWritable;
import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.tasks.TaskState;

/**
 * Wraps an existing monitor/reporter to support nested action calls.
 * Progress messages are sent to the wrapped reporter but the termination
 * status is stored by the wrapper. Has the same ID as the wrapped reporter.
 */
public class NestedProgressReporter implements ProgressMonitorReporter {
    protected ProgressMonitorReporter wrapped;
    protected TaskState state = TaskState.Waiting;
    protected int progress = 0;
    protected boolean succeeded = true;
    
    public NestedProgressReporter(ProgressMonitorReporter monitor) {
        wrapped = monitor;
    }
    
    @Override
    public synchronized void setState(TaskState state) {
        this.state = state;
        if (state == TaskState.Running) {
            progress = 1;
        } else if (state == TaskState.Waiting) {
            progress = 0;
        } else {
            progress = 100;
        }
        notifyAll();
    }

    @Override
    public String getId() {
        return wrapped.getId();
    }

    @Override
    public synchronized void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public synchronized void setSuccess(boolean wasSuccessful) {
        succeeded = wasSuccessful;
    }

    @Override
    public synchronized void setFailed() {
        succeeded = false;
        setState( TaskState.Terminated );
    }

    @Override
    public void setSucceeded() {
        succeeded = true;
        setState(TaskState.Terminated);
    }
    
    @Override
    public synchronized TaskState getState() {
        return state;
    }

    @Override
    public synchronized int getProgress() {
        return progress;
    }

    @Override
    public synchronized boolean succeeded() {
        return succeeded;
    }

    @Override
    public String toString() {
        return String.format("Progress: %d %s(%s)", progress, state, succeeded ? "succeeded" : "failed");
    }

    @Override
    public List<ProgressMessage> getMessages() {
        return wrapped.getMessages();
    }

    @Override
    public List<ProgressMessage> getMessagesSince(int offset) {
        return wrapped.getMessagesSince(offset);
    }

    @Override
    public boolean moreMessagesSince(int offset) {
        return wrapped.moreMessagesSince(offset);
    }

    @Override
    public void report(String message) {
        wrapped.report(message);
    }

    @Override
    public void report(String message, int lineNumber) {
        wrapped.report(message, lineNumber);
    }

    @Override
    public JSONWritable viewUpdatesSince(int offset) {
        return wrapped.viewUpdatesSince(offset);
    }

    @Override
    public void report(String message, String type) {
        wrapped.report(message, type);
    }

    @Override
    public void report(String message, int lineNumber, String type) {
        wrapped.report(message, lineNumber, type);
    }

    @Override
    public void reportError(String message) {
        wrapped.report(message);
        setFailed();
    }

    @Override
    public void reportError(String message, int lineNumber) {
        wrapped.report(message, lineNumber);
        setFailed();
    }
    
}
