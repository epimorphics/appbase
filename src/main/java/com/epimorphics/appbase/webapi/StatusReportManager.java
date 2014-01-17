/******************************************************************
 * File:        StatusReportManager.java
 * Created by:  Dave Reynolds
 * Created on:  15 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.epimorphics.appbase.core.TimerManager;
import com.epimorphics.tasks.SimpleProgressMonitor;

/**
 * Component to help web service provide a persistent, asynchronous status and progress report.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class StatusReportManager {
    protected Map<String, SimpleProgressMonitor> cache = new HashMap<>();
    protected long retentionPeriod = 24*60*60*1000;  // default is 1 day
    protected long reaperInterval  = 10*60*1000;     // default is 10min
    protected long allocationCounter = 0;
    
    protected boolean reaperStarted = false;
    
    /**
     * Configure interval at which clean up reader runs, default is very 10 minutes
     * @param intervalInS the interval (milliseconds)
     */
    public void setReaperInterval(int interval) {
        reaperInterval = interval;
    }
    
    /**
     * Configure min period for which a status report should be kepet, default is 1 day. 
     * @param periodInS Retention period (milliseconds)
     */
    public void setRetentionPeriod(int period) {
        retentionPeriod = period;
    }
    
    /**
     * Create a new retained progress report
     */
    public SimpleProgressMonitor createProgressMonitor() {
        String id = Long.toString( allocationCounter++ );
        SimpleProgressMonitor monitor = new SimpleProgressMonitor( id );
        cache.put(id, monitor);
        startReaper();
        return monitor;
    }
    
    /**
     * Retrieve a previously allocated report
     */
    public SimpleProgressMonitor getProgressMonitor(String id) {
        return cache.get(id);
    }
    
    protected void startReaper() {
        if (!reaperStarted) {
            TimerManager.get().scheduleAtFixedRate(new Reaper(this), reaperInterval, reaperInterval, TimeUnit.MILLISECONDS);
            reaperStarted = true;
        }
    }
    
    final class Reaper implements Runnable {
        protected StatusReportManager parent;
        
        public Reaper(StatusReportManager parent) {
            this.parent = parent;
        }
        
        @Override
        public void run() {
            synchronized (parent) {
                long reapThreshold = System.currentTimeMillis() - retentionPeriod;
                List<String> toReap = new ArrayList<>();
                for (Map.Entry<String, SimpleProgressMonitor> entry : cache.entrySet()) {
                    if (entry.getValue().getTimestamp() < reapThreshold) {
                        toReap.add(entry.getKey());
                    }
                }
                for (String key : toReap) {
                    cache.remove(key);
                }
            }
        }
        
    }
    
}
