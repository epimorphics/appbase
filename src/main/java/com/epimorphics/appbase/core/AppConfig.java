/******************************************************************
 * File:        AppConfig.java
 * Created by:  Dave Reynolds
 * Created on:  22 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.util.EpiException;

/**
 * Instantiates the App(s) by loading their configuration file.
 * <p>
 * Should be included in the web.xml as a listener so that
 * it can fire on webapp startup and have access to the ServletContext.
 * </p>
 * <p>
 * The App(s) are defined by a context parameters whose name matches "AppConfig.appname"
 * and whose value is a comma-separated list of locations to search for the configuration 
 * file for that app. In this, all location-related parameters, the string "${webapp}" will
 * be replaced by the location of the webapp files.
 * </p>
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class AppConfig implements ServletContextListener {
    static Logger log = LoggerFactory.getLogger(AppConfig.class);
    
    public static final String WEBAPP_MACRO = "${webapp}";
    public static final String CONFIG_PREFIX = "AppConfig.";

    public static AppConfig theConfig;

    protected Map<String, App> apps = new HashMap<String, App>();
    protected App defaultApp;
    protected String filebase = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        theConfig = this;           // Keep the last initialized version as the default global config
        ServletContext context = sce.getServletContext();
        filebase =  withoutTrailingSlash(context.getRealPath("/"));

        Enumeration<String> paramNames = context.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            if (param.startsWith(CONFIG_PREFIX)) {
                String appName = param.substring(CONFIG_PREFIX.length());
                String configFiles = context.getInitParameter(param);
                for (String configF : configFiles.split(",") ) {
                    File configFile = new File( expandFileLocation(configF.trim()) );
                    if (configFile.exists() && configFile.canRead()) {
                        try {
                            App app = new App(appName, configFile);
                            apps.put(appName, app);
                            if (defaultApp == null) {
                                defaultApp = app;
                                log.info("Loaded App " + appName + " as the default app");
                            } else {
                                log.info("Loaded App " + appName);
                            }
                        } catch (Exception e) {
                            log.error("Failed to load configuration file: " + configFile, e);
                        }
                    }
                }
            }
        }
        
        if (defaultApp == null) {
            throw new EpiException("No apps successfully configured");
        }
    }
    
    /**
     * Return the global default app.
     */
    public static App get() {
        return getAppConfig().defaultApp;
    }
    
    /**
     * Return the app with the given name
     */
    public static App get(String appName) {
        return getAppConfig().apps.get(appName);
    }

    public static AppConfig getAppConfig() {
        if (theConfig == null) {
            // Should only happen during testing
            theConfig = new AppConfig();
        }
        return theConfig;
    }


    public String expandFileLocation(String location) {
        if (filebase == null) {
            return location;
        }
        return location.replace(WEBAPP_MACRO, filebase );
    }

    private String withoutTrailingSlash(String path) {
        return path.endsWith("/") ? path.substring(0, path.length()-1) : path;
    }

    @Override
    public synchronized void contextDestroyed(ServletContextEvent sce) {
        for (String name : apps.keySet()) {
            apps.get(name).shutdown();
            log.info("Shut down " + name);
        }
    }

}
