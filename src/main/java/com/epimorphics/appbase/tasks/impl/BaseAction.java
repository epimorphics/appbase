/******************************************************************
 * File:        BaseAction.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.DESCRIPTION_KEY;
import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.NAME_KEY;
import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.TIMEOUT_KEY;

import java.util.HashMap;
import java.util.Map;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.tasks.ProgressMonitorReporter;

/**
 * Base class for implementing generic, configurable actions.
 */
public abstract class BaseAction implements Action {
    protected Map<String, Object> configuration = new HashMap<String, Object>();
    protected Action onError;

    public BaseAction() {
    }

    public BaseAction(Map<String, Object> config) {
        configuration.putAll(config);
    }
    
    public void setConfig(String key, Object value) {
        configuration.put(key, value);
    }
    
    public void setConfig(Map<String, Object> config) {
        configuration.putAll(config);
    }

    public Object getConfig(String key) {
        return configuration.get(key);
    }

    public String getStringConfig(String key, String deflt) {
        Object value = configuration.get(key);
        if (value != null) {
            return value.toString();
        } else {
            return deflt;
        }
    }

    public int getIntConfig(String key, int deflt) {
        Object value = configuration.get(key);
        if (value != null && value instanceof Number) {
            return ((Number)value).intValue();
        } else {
            return deflt;
        }
    }

    @Override
    public String getName() {
        return getStringConfig(NAME_KEY, "BaseAction");
    }

    @Override
    public void setName(String name) {
        setConfig(NAME_KEY, name);
    }
    
    public String getDescription() {
        return getStringConfig(DESCRIPTION_KEY, "");
    }
    
    @Override
    public int getTimeout() {
        return getIntConfig(TIMEOUT_KEY, -1);
    }

    public Object getParameter(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            value = configuration.get(key);
        }
        return value;
    }

    public String getStringParameter(Map<String, Object> parameters, String key) {
        return getStringParameter(parameters, key, null);
    }

    public String getStringParameter(Map<String, Object> parameters, String key, String deflt) {
        Object value = getParameter(parameters, key);
        if (value != null) {
            return value.toString();
        } else {
            return deflt;
        }
    }

    public int getIntParameter(Map<String, Object> parameters, String key, int deflt) {
        Object value = getParameter(parameters, key);
        if (value != null && value instanceof Number) {
            return ((Number)value).intValue();
        } else {
            return deflt;
        }
    }

    public boolean getBooleanParameter(Map<String, Object> parameters, String key, boolean deflt) {
        Object value = getParameter(parameters, key);
        if (value != null && value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        } else {
            return deflt;
        }
    }

    @Override
    public void run(Map<String, Object> parameters, ProgressMonitorReporter monitor) {
        doRun(parameters, monitor);
        if ( ! monitor.succeeded() && onError != null ) {
            onError.run(parameters, monitor);
        }
    }
    
    protected abstract void doRun(Map<String, Object> parameters, ProgressMonitorReporter monitor);
 
    /**
     * Merge a set of configurations for a base action into this configuration,
     * no overriding any current settings
     */
    public void mergeBaseConfiguration(Action base) {
        if (base instanceof BaseAction) {
            Map<String, Object> baseConfig = ((BaseAction)base).configuration;
            for (String key : baseConfig.keySet()) {
                if ( ! configuration.containsKey(key) ) {
                    configuration.put(key, baseConfig.get(key));
                }
            }
        }
    }
}
