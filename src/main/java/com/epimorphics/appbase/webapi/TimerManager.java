/******************************************************************
 * File:        Timer.java
 * Created by:  Dave Reynolds
 * Created on:  15 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import java.util.Timer;

/**
 * Global singleton timer used for scheduling both one-off and periodic tasks.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TimerManager {

    public static Timer theTimer;
    
    public static Timer get() {
        if (theTimer == null) {
            theTimer = new Timer(true);
        }
        return theTimer;
    }
}
