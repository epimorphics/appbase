/******************************************************************
 * File:        ProgressMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  11 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.util.List;

/**
 * Interface for accessing progress status from an async task such
 * as a web service or shell script.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface ProgressMonitor {
    
    /**
     * Return the state of the task. It may be Waiting to start,
     * Running or Terminated.
     */
    public TaskState getState();
    
    /**
     * Get the progress of the task as a percentage. Not all
     * monitors will offer fine grained progress but the progress 
     * will always be 0 for Waiting tasks, >= 1 for running tasks and 100
     * for Terminated tasks (even for unsuccessful termination).
     */
    public int getProgress();
    
    /**
     * Return true if the task has Terminated and was successful.
     */
    public boolean succeeded();
    
    /**
     * Return all progress messages logged so far. 
     */
    public List<ProgressMessage> getMessages();
    
    /**
     * Return all progress messages since the given message number.
     */
    public List<ProgressMessage> getMessagesSince(int offset);

    /**
     * Return true if there are more progress messages available since the given message number.
     */
    public boolean moreMessagesSince(int offset);
}
