/******************************************************************
 * File:        BaseAction.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import static com.epimorphics.appbase.tasks.ActionJsonFactorylet.*;
import static com.epimorphics.json.JsonUtil.*;

import java.util.Map.Entry;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.atlas.json.JsonValue;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionJsonFactorylet;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.appbase.tasks.ActionTrigger;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;

/**
 * Base class for implementing generic, configurable actions.
 */
public abstract class BaseAction implements Action {
    protected JsonObject configuration;
    protected Action onError;
    protected Action onSuccess;
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

    @Override 
    public JsonObject getConfig() {
        return configuration;
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

    public boolean getBooleanConfig(String key, boolean deflt) {
        return getBooleanValue(configuration, key, deflt);
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

    public JsonValue getParameter(JsonObject parameters, String key) {
        JsonValue value = parameters.get(key);
        // No longer needed, doRun should always see the merged configuration plus parameters
//        if (value == null) {
//            value = configuration.get(key);
//        }
        return value;
    }
    
    public JsonValue getRequiredParameter(JsonObject parameters, String key) {
        JsonValue v = getParameter(parameters, key);
        if (v == null) {
            throw new EpiException("Action could not find required parameter: " + key);
        }
        return v;
    }
    
    public String getStringParameter(JsonObject parameters, String key, String deflt) {
        JsonValue v = getParameter(parameters, key);
        if (v != null && v.isString()) {
            return v.getAsString().value();
        } else {
            return deflt;
        }
    }
    
    public String getStringParameter(JsonObject parameters, String key) {
        JsonValue v = getParameter(parameters, key);
        if (v != null && v.isString()) {
            return v.getAsString().value();
        } else {
            throw new EpiException("Action could not find required parameter: " + key);
        }
    }
    
    public int getIntParameter(JsonObject parameters, String key, int deflt) {
        JsonValue v = getParameter(parameters, key);
        if (v != null && v.isNumber()) {
            return v.getAsNumber().value().intValue();
        } else {
            return deflt;
        }
    }
    
    public boolean getBooleanParameter(JsonObject parameters, String key, boolean deflt) {
        JsonValue v = getParameter(parameters, key);
        if (v != null && v.isBoolean()) {
            return v.getAsBoolean().value();
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
    public Action getOnSuccess() {
        return onSuccess;
    }
    
    
    public void setOnError(Action onError) {
        this.onError = onError;
    }

    public void setOnSuccess(Action onSuccess) {
        this.onSuccess = onSuccess;
    }

    @Override
    public void resolve(ActionManager am) {
        if (onError == null) {
            onError = resolveAction(am, getConfig(ON_ERROR_KEY));
        }
        if (onSuccess == null) {
            onSuccess = resolveAction(am, getConfig(ON_SUCCESS_KEY));
        }
    }

    protected Action resolveAction(ActionManager am, Object action) {
        Action resolved;
        if (action == null) {
            return null;
        } else if (action instanceof String) {
            resolved = am.get((String)action);
        } else if (action instanceof JsonString) {
            resolved = am.get( ((JsonString)action).value() );
        } else if (action instanceof Action) {
            resolved = (Action)action;
        } else if (action instanceof JsonObject) {
            resolved = ActionJsonFactorylet.parseAction((JsonObject)action);
        } else {
            throw new EpiException("Unexpected type for bound action (should be String or Action): " + action);
        }
        if (resolved == null) {
            return null;
        }
        resolved.resolve(am);
        return resolved;
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
        return doRun( mergedCall(parameters), monitor);
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
        if (onError == null) {
            onError = base.getOnError();
        }
        if (onSuccess == null) {
            onSuccess = base.getOnSuccess();
        }
    }
    
    public JsonObject error(String message, ProgressMonitorReporter monitor) {
        monitor.reportError(message);
        return JsonUtil.emptyObject();
    }

    /**
     * In calls the non-@ configuration overrides parameters to allow us e.g. bind a messages into a message action
     * and not have it overridden with messages for other actions when chaining.
     */
    protected JsonObject mergedCall(JsonObject parameters) {
        JsonObject call = JsonUtil.makeJson(parameters);
        for (Entry<String, JsonValue> e : configuration.entrySet()) {
            String key = e.getKey();
            if ( ! key.startsWith("@") || ! call.hasKey(key)) {
                call.put(key, e.getValue());
            }
        }
        return call;
    }
    
    /**
     * Utility to strip @-configuration keys for a call to make it safe
     * to use in invoking a nested action
     */
    public JsonObject makeSafe(JsonObject params) {
        JsonObject safe = new JsonObject();
        for ( Entry<String, JsonValue> e  : params.entrySet()) {
            if (!e.getKey().startsWith("@")) {
                safe.put(e.getKey(), e.getValue());
            }
        }
        return safe;
    }
}
