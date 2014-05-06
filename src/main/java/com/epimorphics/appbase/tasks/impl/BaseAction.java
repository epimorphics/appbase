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
import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.ON_ERROR_KEY;
import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.TIMEOUT_KEY;

import java.util.HashMap;
import java.util.Map;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.appbase.tasks.ActionTrigger;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;

/**
 * Base class for implementing generic, configurable actions.
 */
public abstract class BaseAction implements Action {
    protected Map<String, Object> configuration = new HashMap<String, Object>();
    protected Action onError;
    protected ActionTrigger trigger;

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
    
    public Action getActionNamed(String name) {
        ActionManager am = AppConfig.getApp().getA(ActionManager.class);
        Action a = am.get(name);
        if (a == null) {
            throw new EpiException("Can't find the action named: " + name);
        }
        return a;
    }
    
    @Override
    public Action getOnError() {
        return onError;
    }
    
    @Override
    public void resolve(ActionManager am) {
        onError = resolveAction(am, getConfig(ON_ERROR_KEY)); 
    }

    protected Action resolveAction(ActionManager am, Object action) {
        if (action == null) {
            return null;
        } else if (action instanceof String) {
            return am.get((String)action);
        } else if (action instanceof Action) {
            return (Action)action;
        } else {
            throw new EpiException("Unexpected type for bound action (should be String or Action): " + action);
        }
    }
    
    @Override
    public ActionTrigger getTrigger() {
        return trigger;
    }
    
    public void setTrigger(ActionTrigger trigger) {
        this.trigger = trigger;
    }
    
    @Override
    public Map<String, Object> run(Map<String, Object> parameters, ProgressMonitorReporter monitor) {
        return doRun(parameters, monitor);
    }
    
    protected abstract Map<String, Object> doRun(Map<String, Object> parameters, ProgressMonitorReporter monitor);
 
    /**
     * Merge a set of configurations for a base action into this configuration,
     * not overriding any current settings
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
    
    /**
     * Return a new map which merges the two argument maps with the extra values overriding any equivalently named params.
     */
    public static Map<String, Object> merge(Map<String, Object> params, Map<String, Object> extra) {
        Map<String, Object> result = new HashMap<>( params );
        result.putAll(extra);
        return result;
    }
}
