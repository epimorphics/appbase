/******************************************************************
 * File:        BindingEnv.java
 * Created by:  Dave Reynolds
 * Created on:  18 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a set of name to value mappings. Can be chained so it's 
 * possible to efficiently override some values in a local environment.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class BindingEnv extends HashMap<String, Object> implements Map<String, Object> {
    private static final long serialVersionUID = 1L;
    
    protected BindingEnv parent;
    
    /**
     * Create an empty, standalone environment
     */
    public BindingEnv() {
        super();
    }
    
    /**
     * Create an environment which inherits from a parent
     * environment. Locally asserted bindings will override
     * bindings from the parent. 
     */
    public BindingEnv(BindingEnv parent) {
        this.parent = parent;
    }
    
    /**
     * Create an environment with initial values given by the map
     */
    public BindingEnv(Map<String, Object> map) {
        super(map);
    }
    
    /**
     * Create an environment with initial values given key=value,key-2=value2 ...
     */
    public BindingEnv(String bindings) {
        super();
        for (String binding : bindings.split(",")) {
            String[] pair = binding.split("=");
            String key = pair[0].trim();
            String value = pair[1].trim();
            try {
                int vint = Integer.parseInt(value);
                put(key, vint);
            } catch (NumberFormatException e) {
                put(key, value);
            }
        }
    }
    
    @Override
    public Object get(Object key) {
        return doGet(key);
    }
    
    public Object get(String key, Object deflt) {
        Object value = get(key);
        if (value == null) {
            value = deflt;
        }
        return value;
    }

    /**
     * Return the most recent binding of a key or null if there is one (even 
     * if there is an inherited value further up the chain).
     */
    public Object getLocal(String name) {
        return super.get(name);
    }
    
    /**
     * Return all keys to which a value is bound
     */
    public Set<String> getKeys() {
        if (parent != null) {
            Set<String> keys = new HashSet<>();
            addKeys(keys);
            return keys;
        } else {
            return keySet();
        }
    }
    
    private void addKeys(Set<String> keys) {
        keys.addAll( keySet() );
        if (parent != null) {
            parent.addKeys(keys);
        }
    }
    
    // Sometimes the java type system just seems to work against you
    private Object doGet(Object key) {
        Object v = super.get(key);
        if (v == null && parent != null) {
            return parent.get(key);
        }
        return v;
    }
    
    public void setParent(BindingEnv parent) {
        this.parent = parent;
    }
    
    public BindingEnv getParent() {
        return parent;
    }
}
