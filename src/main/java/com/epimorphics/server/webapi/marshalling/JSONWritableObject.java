/******************************************************************
 * File:        JSONWritableObject.java
 * Created by:  Dave Reynolds
 * Created on:  15 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.server.webapi.marshalling;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A streaming JSON object. A key/value map whose values are all
 * JSONWritables.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class JSONWritableObject implements JSONWritable {

    protected Map<String, JSONWritable> object = new HashMap<String, JSONWritable>();
    
    public JSONWritableObject() {
    }
    
    public void put(String key, JSONWritable value) {
        object.put(key, value);
    }
    
    public JSONWritable get(String key) {
        return object.get(key);
    }
    
    public void put(String key, String value) {
        if (value != null) {
            put(key, new WrappedString(value));
        }
    }

    public void put(String key, List<JSONWritable> values) {
        if (values != null) {
            put(key, new WrappedList(values));
        }
    }

    @Override
    public void writeTo(JSFullWriter out) {
        out.startObject();
        for (String key : object.keySet()) {
            out.key(key);
            object.get(key).writeTo(out);
        }
        out.finishObject();
    }
    
    static class WrappedString implements JSONWritable {
        protected String value;
        public WrappedString(String value) {
            this.value = value;
        }
        @Override
        public void writeTo(JSFullWriter out) {
            out.print( JSFullWriter.outputQuotedString(value) );
        }
    }
    
    static class WrappedList implements JSONWritable {
        protected List<JSONWritable> values;
        public WrappedList(List<JSONWritable> values) {
            this.values = values;
        }
        @Override
        public void writeTo(JSFullWriter out) {
            out.startArray();
            for (Iterator<JSONWritable> i = values.iterator(); i.hasNext();) {
                i.next().writeTo(out);
                if (i.hasNext()) {
                    out.arraySep();
                }
            }
            out.finishArray();
        }
    }

}
