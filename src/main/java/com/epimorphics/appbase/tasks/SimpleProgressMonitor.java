/******************************************************************
 * File:        SimpleProgressMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  11 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation of progress monitor/reporter for in-process reporting.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class SimpleProgressMonitor implements ProgressMonitor, ProgressReporter {
    protected TaskState state = TaskState.Waiting;
    protected int progress = 0;
    protected boolean succeeded = false;
    protected List<ProgressMessage> messages = new ArrayList<>();
    
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
    public synchronized void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public synchronized void setSuccess(boolean wasSuccessful) {
        succeeded = wasSuccessful;
    }

    @Override
    public synchronized void suceeeded() {
        setState( TaskState.Terminated );
        succeeded = true;
    }

    @Override
    public synchronized void failed() {
        setState( TaskState.Terminated );
        succeeded = false;
    }

    @Override
    public synchronized void report(String message) {
        messages.add( new ProgressMessage(message) );
    }

    @Override
    public synchronized void report(String message, int lineNumber) {
        messages.add( new ProgressMessage(message, lineNumber) );
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
    public synchronized List<ProgressMessage> getMessages() {
        return messages;
    }

    @Override
    public synchronized List<ProgressMessage> getMessagesSince(int offset) {
        return messages.subList(offset, messages.size());
    }

    @Override
    public synchronized boolean moreMessagesSince(int offset) {
        return messages.size() > offset;
    }

}
