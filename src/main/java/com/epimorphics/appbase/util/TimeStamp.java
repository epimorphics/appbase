/******************************************************************
 * File:        TimeStamp.java
 * Created by:  Dave Reynolds
 * Created on:  10 May 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trivial utility for creating URIs based on a timestamp. 
 */
public class TimeStamp {
    protected static AtomicInteger disambig = new AtomicInteger(0);
    
    public static String makeTimestamp(long time) {
        disambig.compareAndSet(1000, 0);  // limit to 1000 distinct updates per ms
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-S").format( new Date(time) ) + disambig.getAndIncrement();
    }
    
    public static String makeTimestamp() {
        disambig.compareAndSet(1000, 0);  // limit to 1000 distinct updates per ms
        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-S").format( new Date() ) + disambig.getAndIncrement();
    }
    
}
