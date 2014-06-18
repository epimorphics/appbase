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
        disambig.compareAndSet(9999, 0);  // allows for 10000 distinct updates per ms
        return String.format("%s-%04d", 
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS").format( new Date(time) ),
                disambig.getAndIncrement());
    }
    
    public static String makeTimestamp() {
        return makeTimestamp( System.currentTimeMillis() );
    }
    
}
