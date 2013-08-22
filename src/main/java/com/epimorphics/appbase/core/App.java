/******************************************************************
 * File:        App.java
 * Created by:  Dave Reynolds
 * Created on:  22 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.util.EpiException;

/**
 * An App is a set of configured components and global configuration parameters. 
 * There can be more than one App within a single webapp but typically there is 
 * just one which is available as a global singleton.
 * <p>
 * Apps are configured through a properties like file.
 * Use # to start a comment line
 * <pre>
 * # a comment line
 * </pre>
 * To set a configuration variable for the app itself:
 * <pre>
 * app.var = value
 * </pre>
 * To create a component:
 * <pre>
 * componentName = com.epimorphics.pacakge.MyBean
 * </pre>
 * To set a configuration value on a component:
 * <pre>
 * componentName.prop = 42
 * componentName.prop = value
 * componentName.prop = string with spaces
 * </pre>
 * The "prop" can be a dotted sequence of prop names (p.q.r) a map access (p("key"))
 * or an array access (p[3]) as defined by Apache BeanUtils).
 * <p>
 * To set link one component to another use
 * <pre>
 * componetName.prop = $otherComponentName
 * </pre>
 * where the referenced component needs to have been created earlier in the file.
 * </p>
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */

// TODO ability to set list values with comma separated list

public class App {
    static Logger log = LoggerFactory.getLogger(App.class);
    
    protected String name;
    protected Map<String, Object> config = new HashMap<>();
    protected Map<String, Object> components = new HashMap<String, Object>();

    public App(String name, File config) throws IOException {
        this.name = name;
        loadConfig(config);
        startup();
    }
    
    /**
     * Configured name for the app
     */
    public String getName() {
        return name;
    }

    /**
     * List of component names
     */
    public Collection<String> listComponentNames() {
        return components.keySet();
    }

    /**
     * Get a named component
     */
    public Object getComponent(String name ) {
        return components.get(name);
    }

    /**
     * Return a named component coerced to an expected class
     */
    @SuppressWarnings("unchecked")
    public <T> T getComponentAs(String name, Class<T> cls ) {
        Object s = components.get(name);
        if (s != null && cls.isInstance(s)) {
            return (T)s;
        } else {
            return null;
        }
    }

    /**
     * Return the a component with the given class if there is one,
     * otherwise return null 
     */
    @SuppressWarnings("unchecked")
    public <T> T getA(Class<T> cls) {
        for (Object s : components.values()) {
            if (cls.isInstance(s)) {
                return (T)s;
            }
        }
        return null;
    }
    
    /**
     * Return the list of configuration parameters
     */
    public Collection<String> listParams() {
        return config.keySet();
    }

    /**
     * Get a configuration parameter
     */
    public Object getParam(String name ) {
        return config.get(name);
    }

    /**
     * Gracefully shutdown any components that implement
     * the Shutdown signature.
     */
    public void shutdown() {
        for (String cname : components.keySet()) {
            Object component= components.get(cname);
            if (component instanceof Shutdown) {
                ((Shutdown)component).shutdown();
                log.info("Shut down " + name + "." + cname);
            }
        }
    }

    /**
     * Run any components that should be run at startup
     */
    public void startup() {
        for (String cname : components.keySet()) {
            Object component= components.get(cname);
            if (component instanceof Startup) {
                ((Startup)component).startup(this);
            }
        }
    }
    
    // ---- Configuration file processor --------------
    
    static final String APP_PARAM_PREFIX = "app.";
    
    protected void loadConfig(File configF) throws IOException {
        BufferedReader in = new BufferedReader( new FileReader(configF) );
        String line = null;
        int lineNum = 0;
        while ((line = in.readLine()) != null) {
            processConfigLine(lineNum++, line.trim());
        }
        in.close();
    }
    
    protected void processConfigLine(int lineNum, String line) {
        if (line.startsWith("#")) return;  // Pure comment line
        if (line.isEmpty()) return;   // Skip empty lines
        
        String[] parts = line.split("=");
        if (parts.length != 2)  error(lineNum, line, "expected a '=' assignment");

        String target = parts[0].trim();
        Object value = asValue( parts[1].trim() );
        
        if (target.startsWith(APP_PARAM_PREFIX)) {
            // Global config setting
            String param = target.substring(APP_PARAM_PREFIX.length());
            config.put(param, value);
            
        } else if (target.contains(".")) {
            // set a value on an existing component
            int split = target.indexOf('.');
            String componentName = target.substring(0, split);
            Object component = components.get(componentName);
            if (component == null) {
                error(lineNum, line, "could not find component '" + componentName + "'");
            }
            String prop = target.substring(split + 1);
            try {
                PropertyUtils.setSimpleProperty(component, prop, value);
            } catch (Exception e) {
                error(lineNum, line, "Component " + componentName + " does not have property " + prop);
            }
            
        } else {
            // Create a new component
            try {
                Object component = Class.forName( value.toString() ).newInstance();
                components.put(target, component);
            } catch (Exception e) {
                error(lineNum, line, "Failed to instantiate component: " + value, e);
            }
        }
    }
    
    protected Object asValue(String valueName) {
        if (valueName.startsWith("$")) {
            return components.get( valueName.substring(1) );
        }
        try {
            return Long.parseLong(valueName);
        } catch (NumberFormatException e) {
            return valueName;
        }
    }
    
    protected void error(int lineNum, String line, String error, Exception e) {
        throw new EpiException(String.format("Error in config file for %s (line %d) - %s : %s", name, lineNum, error, line), e);
    }
    
    protected void error(int lineNum, String line, String error) {
        throw new EpiException(String.format("Error in config file for %s (line %d) - %s : %s", name, lineNum, error, line));
    }
}
