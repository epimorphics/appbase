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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.shared.PrefixMapping;

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
 * componentName.prop = true
 * componentName.prop = value
 * componentName.prop = string with spaces
 * componentName.prop = "a string with surround quotes omitted"
 * </pre>
 * The "prop" can be a dotted sequence of prop names (p.q.r) a map access (p("key"))
 * or an array access (p[3]) as defined by Apache BeanUtils).
 * <p>
 * To set link one component to another use a $ prefix:
 * <pre>
 * componentName.prop = $otherComponentName
 * </pre>
 * To set a list of links to other components use comma-separated values:
 * <pre>
 * componentName.prop = $otherComponentName, $anotherone
 * </pre>
 * where the referenced component needs to have been created earlier in the file.
 * </p>
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class App {
    static Logger log = LoggerFactory.getLogger(App.class);
    
    public static final String LANGUAGE_PROP = "language";
    public static final String DEAFULT_LANGUAGE = "en";
    
    protected String name;
    protected Map<String, Object> config = new HashMap<>();
    protected Map<String, Object> components = new HashMap<String, Object>();
    
    protected PrefixService prefixService;

    /**
     * Construct an App instance configured by a file.
     */
    public App(String name, File config) throws IOException {
        this.name = name;
        loadConfig(config);
        startup();
    }

    /**
     * Construct a raw app instance used for testing.
     * @param name
     */
    public App(String name) {
        this.name = name;
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
     * Register a new component. Mostly used for testing.
     */
    public void addComponent(String name, Object component) {
        components.put(name, component);
    }
    
    /**
     * Return the configured prefixes
     */
    public PrefixMapping getPrefixes() {
        if (prefixService == null) {
            prefixService = getA(PrefixService.class);
            if (prefixService == null) {
                prefixService = new PrefixService();
            }
        }
        return prefixService.getPrefixes();
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
     * Set a configuration parameter. Mostly used for testing.
     */
    public void setParam(String param, Object value) {
        config.put(param, value);
    }
    
    /**
     * Get a configuration parameter with default
     */
    public Object getParam(String name, Object dflt) {
        Object val = config.get(name);
        if (val == null) {
            val = dflt;
        }
        return val;
    }

    /**
     * Get language for this app. Set with "language" configuration parameter.
     */
    public String getLanguage() {
        return getParam(LANGUAGE_PROP, DEAFULT_LANGUAGE).toString();
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
                error(lineNum, line, "Component " + componentName + " does not have property " + prop + ", " + e);
            }
            
        } else {
            // Create a new component
            try {
                String name = value.toString();
                Object component = Class.forName( name ).newInstance();
                if (component instanceof Named) {
                    ((Named)component).setName(target);
                }
                components.put(target, component);
            } catch (Exception e) {
                error(lineNum, line, "Failed to instantiate component: " + value, e);
            }
        }
    }
    
    protected Object asValue(String valueName) {
        if (valueName.startsWith("$")) {
            if (valueName.contains(",")) {
                // Array of reference values
                List<Object> values = new ArrayList<Object>();
                for (String name :  valueName.split(",")) {
                    values.add( asValue(name.trim()) );
                }
                return values;
            }
            Object component = components.get( valueName.substring(1) );
            if (component == null) {
                throw new EpiException("Reference to " + valueName + " not found");
            }
            return component;
        }
        if (valueName.startsWith("\"") && valueName.endsWith("\"")) {
            return valueName.substring(1, valueName.length()-2);
        }
        if (valueName.equalsIgnoreCase("true")) {
            return true;
        } else if (valueName.equalsIgnoreCase("false")) {
            return false;
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
