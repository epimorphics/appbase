/******************************************************************
 * File:        Timer.java
 * Created by:  Dave Reynolds
 * Created on:  15 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Global singleton timer used for scheduling both one-off and periodic tasks.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TimerManager {

    public static ScheduledExecutorService theTimer;
    
    public static ScheduledExecutorService get() {
        if (theTimer == null) {
            theTimer = new ScheduledThreadPoolExecutor(1);
        }
        return theTimer;
    }
}
