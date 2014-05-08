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

import java.util.Map;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionJsonFactorylet;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.appbase.tasks.ActionTrigger;
import static com.epimorphics.json.JsonUtil.*;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;

/**
 * Base class for implementing generic, configurable actions.
 */
public abstract class BaseAction implements Action {
    protected JsonObject configuration;
    protected Action onError;
    protected ActionTrigger trigger;

    public BaseAction() {
        configuration = new JsonObject();
    }

    public BaseAction(JsonObject config) {
        configuration = makeJson(config);
    }
    
    public void setConfig(String key, Object value) {
        configuration.put(key, asJson(value));
    }
    
    public void setConfig(JsonObject config) {
        mergeInto(configuration, config);
    }

    public Object getConfig(String key) {
        return configuration.get(key);
    }

    public String getStringConfig(String key, String deflt) {
        return getStringValue(configuration, key, deflt);
    }

    public int getIntConfig(String key, int deflt) {
        return getIntValue(configuration, key, deflt);
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
        } else if (action instanceof JsonObject) {
            return ActionJsonFactorylet.parseAction((JsonObject)action);
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
    public JsonObject run(JsonObject parameters, ProgressMonitorReporter monitor) {
        return doRun(parameters, monitor);
    }
    
    protected abstract JsonObject doRun(JsonObject parameters, ProgressMonitorReporter monitor);
 
    /**
     * Merge a set of configurations for a base action into this configuration,
     * not overriding any current settings
     */
    public void mergeBaseConfiguration(Action base) {
        if (base instanceof BaseAction) {
            configuration = merge(((BaseAction)base).configuration, configuration);
        }
    }

}
