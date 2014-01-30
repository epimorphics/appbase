/******************************************************************
 * File:        Timer.java
 * Created by:  Dave Reynolds
 * Created on:  15 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Global singleton timer used for scheduling both one-off and periodic tasks.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TimerManager {

    public static ScheduledExecutorService theTimer;
    
    public synchronized static ScheduledExecutorService get() {
        if (theTimer == null) {
            theTimer = new ScheduledThreadPoolExecutor(1);
        }
        return theTimer;
    }
    
    /**
     * Restart the global timer. Used by test cases to restart the timer
     * after it has been shutdown by a servlet-context cleanup.
     */
    public synchronized static void shutdown() {
        if (theTimer != null) {
            theTimer.shutdown();
            theTimer = null;
        }
    }
}
