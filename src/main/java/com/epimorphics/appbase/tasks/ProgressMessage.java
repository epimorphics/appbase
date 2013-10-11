/******************************************************************
 * File:        ProgressReort.java
 * Created by:  Dave Reynolds
 * Created on:  11 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import com.epimorphics.appbase.json.JSFullWriter;
import com.epimorphics.appbase.json.JSONWritable;

/**
 * Simple progress message format which can be serialized to JSON.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ProgressMessage implements JSONWritable {
    protected static final int NULL_LINE_NUMBER = -1;
    
    String message;
    long timestamp;
    int lineNumber;
    
    public ProgressMessage(String message) {
        this(message, NULL_LINE_NUMBER);
    }
    
    public ProgressMessage(String message, int lineNumber) {
        this.message = message;
        this.lineNumber = lineNumber;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return String.format("%tT.%tL %s", timestamp, timestamp, message) + (lineNumber == NULL_LINE_NUMBER ? "" : " [" + lineNumber + "]");
    }

    @Override
    public void writeTo(JSFullWriter out) {
        out.startObject();
        out.pair("timestamp", timestamp);
        out.pair("message", message);
        if (lineNumber != NULL_LINE_NUMBER) {
            out.pair("line", lineNumber);
        }
        out.finishObject();
    }
    
}
