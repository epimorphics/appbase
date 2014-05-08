/******************************************************************
 * File:        HashUtil.java
 * Created by:  Dave Reynolds
 * Created on:  5 May 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.util;

import java.util.HashMap;
import java.util.Map;

import com.epimorphics.util.EpiException;

/**
 * Utilities for working with map parameters, useful for actions
 */
public class MapUtil {

    /**
     * Create a map.
     * @param args alternating parameter names and parameter values
     */
    public static Map<String, Object> makeMap(Object...args) {
        return makeMap(null, args);
    }
    
    /**
     * Copy an existing map adding additional parameter values
     * (which should be alternating parameter name/value pairs)
     */
    public static Map<String, Object> makeMap(Map<String, Object> base, Object...args) {
        if (args.length % 2 != 0) {
            throw new EpiException("MakeMap requires an even number of arguments");
        }
        Map<String, Object> result = base == null ? new HashMap<String, Object>( ) : new HashMap<String, Object>( base );
        for (int i = 0; i < args.length; ) {
            Object paramname = args[i++];
            Object paramval = args[i++];
            result.put(paramname.toString(), paramval);
        }
        return result;
    }
}
