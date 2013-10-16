/******************************************************************
 * File:        ProgressReort.java
 * Created by:  Dave Reynolds
 * Created on:  11 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import static com.epimorphics.appbase.json.JsonUtil.getIntValue;
import static com.epimorphics.appbase.json.JsonUtil.getLongValue;
import static com.epimorphics.appbase.json.JsonUtil.getStringValue;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.json.JSFullWriter;
import com.epimorphics.appbase.json.JSONWritable;

/**
 * Simple progress message format which can be serialized to JSON.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ProgressMessage implements JSONWritable {
    public static final String MESSAGE_FIELD     = "message";
    public static final String LINE_NUMBER_FIELD = "lineNumber";
    public static final String TIMESTAMP_FIELD   = "timestamp";
    
    protected static final int NULL_LINE_NUMBER = -1;
    
    String message;
    long timestamp;
    int lineNumber;
    
    public ProgressMessage(String message) {
        this(message, NULL_LINE_NUMBER);
    }
    
    public ProgressMessage(String message, int lineNumber) {
        this(message, lineNumber, System.currentTimeMillis());
    }
    
    public ProgressMessage(String message, int lineNumber, long timestamp) {
        this.message = message;
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
    }
    
    public ProgressMessage(JsonObject json) {
        this(   getStringValue(json, MESSAGE_FIELD, ""),
                getIntValue(json, LINE_NUMBER_FIELD, NULL_LINE_NUMBER),
                getLongValue(json, TIMESTAMP_FIELD, System.currentTimeMillis())
                );
    }
    
    @Override
    public String toString() {
        return String.format("%tT.%tL %s", timestamp, timestamp, message) + (lineNumber == NULL_LINE_NUMBER ? "" : " [" + lineNumber + "]");
    }

    @Override
    public void writeTo(JSFullWriter out) {
        out.startObject();
        out.pair(TIMESTAMP_FIELD, timestamp);
        out.pair(MESSAGE_FIELD, message);
        if (lineNumber != NULL_LINE_NUMBER) {
            out.pair(LINE_NUMBER_FIELD, lineNumber);
        }
        out.finishObject();
    }
    
}
