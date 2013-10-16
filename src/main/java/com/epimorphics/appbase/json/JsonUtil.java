/******************************************************************
 * File:        JsonUtil.java
 * Created by:  Dave Reynolds
 * Created on:  16 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.json;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;

public class JsonUtil {

    public static String getStringValue(JsonObject jo, String field, String dflt) {
        JsonValue val = jo.get(field);
        if (val == null || !val.isString()) {
            return dflt;
        } else {
            return val.getAsString().value();
        }
    }

    public static int getIntValue(JsonObject jo, String field, int dflt) {
        JsonValue val = jo.get(field);
        if (val == null || !val.isNumber()) {
            return dflt;
        } else {
            return val.getAsNumber().value().intValue();
        }
    }

    public static long getLongValue(JsonObject jo, String field, long dflt) {
        JsonValue val = jo.get(field);
        if (val == null || !val.isNumber()) {
            return dflt;
        } else {
            return val.getAsNumber().value().longValue();
        }
    }

    public static boolean getBooleanValue(JsonObject jo, String field, boolean dflt) {
        JsonValue val = jo.get(field);
        if (val == null || !val.isBoolean()) {
            return dflt;
        } else {
            return val.getAsBoolean().value();
        }
    }
}
