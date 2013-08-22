/******************************************************************
 * File:        Manager.java
 * Created by:  Dave Reynolds
 * Created on:  27 Nov 2012
 *
 * (c) Copyright 2012, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.server.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.server.indexers.LuceneIndex;
import com.epimorphics.server.templates.VelocityRender;
import com.epimorphics.util.EpiException;

/**
 * Controller object which interprets the server configuration and
 * instantiates the requested services. Supports a distinguished
 * service "store" for the default shared RDF dataset.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ServiceConfig implements ServletContextListener {
    static Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    public static final String WEBAPP_MACRO = "${webapp}";
    public static final String CONFIG_PREFIX = "config.";
    public static final String CONFIG_FILE = "service-config";
    public static final String CLASSNAME_PARAM = "class";

    public static final String STORE_SERVICENAME = "store";

    protected Map<String, Service> services = new HashMap<String, Service>();
    protected String filebase = null;

    protected Store defaultStore;

    public static ServiceConfig theConfig;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        theConfig = this;           // Keep the last initialized version as the default global config
        ServletContext context = sce.getServletContext();
        filebase =  withoutTrailingSlash(context.getRealPath("/"));

        String configFiles = context.getInitParameter(CONFIG_FILE);
        if (configFiles != null) {
            for (String configF : configFiles.split("\\|") ) {
                File configFile = new File( expandFileLocation(configF) );
                if (configFile.exists() && configFile.canRead()) {
                    try {
                        parseConfig(configFile, context);
                        break;
                    } catch (IOException e) {
                        log.error("Failed to load configuration file: " + configFile);
                    }
                }
            }
        } else {
            // Load configuration as a set of individual context params
            Enumeration<String> paramNames = context.getInitParameterNames();
            while (paramNames.hasMoreElements()) {
                String param = paramNames.nextElement();
                if (param.startsWith(CONFIG_PREFIX)) {
                    String serviceName = param.substring(CONFIG_PREFIX.length());
                    Service service = parseInit(serviceName, context.getInitParameter(param), context);
                    services.put(serviceName, service);
                }
            }
        }
        
        postInit();
        defaultStore = null;
    }

    // Post-init pass to allow cross linking
    public void postInit() {
        for (String serviceName : services.keySet()) {
            Service service = services.get(serviceName);
            if (service != null) {
                service.postInit();
            }
        }
        for (String serviceName : services.keySet()) {
            Service service = services.get(serviceName);
            if (service != null) {
                service.postPostInit();
            }
        }
    }

    public static ServiceConfig get() {
        if (theConfig == null) {
            // Should only happen during testing
            theConfig = new ServiceConfig();
        }
        return theConfig;
    }

    /**
     * Used in test harnesses. Arg list should be an alternating list of names and services.
     * Each will be added to the configuration and then postInit called for each.
     */
    public void initServices(Object... args) {
        try {
            services.clear();
            int i = 0;
            while (i < args.length) {
                String name = (String)args[i++];
                Service service = (Service)args[i++];
                services.put(name, service);
            }
        } catch (Throwable t) {
            throw new EpiException("Ill-formed arglist to initServices");
        }
        postInit();
    }

    /**
     * Used in test harnesses to clear out set of services
     */
    public void clearServices() {
        services.clear();
    }

    synchronized public List<String> getServiceNames() {
        return new ArrayList<String>( services.keySet() );
    }

    synchronized public Service getService(String name ) {
        return services.get(name);
    }

    @SuppressWarnings("unchecked")
    synchronized public <T> T getServiceAs(String name, Class<T> cls ) {
        Service s = services.get(name);
        if (s != null && cls.isInstance(s)) {
            return (T)s;
        } else {
            return null;
        }
    }

    public Store getDefaultStore() {
        if (defaultStore == null) {
            Service defaultStoreService = getService(STORE_SERVICENAME);
            if (defaultStoreService != null && defaultStoreService instanceof Store) {
                defaultStore = (Store) defaultStoreService;
            }
        }
        if (defaultStore == null) {
            throw new EpiException("No default store defined");
        }
        return defaultStore;
    }

    public LuceneIndex getDefaultIndex() {
        return getFirst(LuceneIndex.class);
    }

    public VelocityRender getDefaultRenderer() {
        return getFirst(VelocityRender.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T getFirst(Class<T> cls) {
        for (Service s : services.values()) {
            if (cls.isInstance(s)) {
                return (T)s;
            }
        }
        return null;
    }
    
    /**
     * Load a service configuration from a Properties file
     */
    public void parseConfig(File configFile, ServletContext context) throws FileNotFoundException, IOException {
        FileReader in = new FileReader(configFile);
        Properties config = new Properties();
        config.load(in);
        in.close();
        parseConfigProperties(config, context);
    }

    
    /**
     * Load a service configuration from a Properties object
     */
    public void parseConfigProperties(Properties config, ServletContext context) {
        Map<String, Service> serviceObjects = new HashMap<String, Service>();
        Map<String, Map<String,String>> configs = new HashMap<String, Map<String,String>>();
        for (String propName : config.stringPropertyNames() ) {
            String value = config.getProperty(propName).trim();
            if (propName.contains(".")) {
                String[] parts = propName.split("\\.");
                String serviceName = parts[0];
                String prop = parts[1];
                Map<String, String> serviceConfig = configs.get(serviceName);
                if (serviceConfig == null) {
                    serviceConfig = new HashMap<String, String>();
                    configs.put(serviceName, serviceConfig);
                }
                serviceConfig.put(prop, value);
            } else {
                try {
                    Service service = (Service) Class.forName( value ).newInstance();
                    serviceObjects.put(propName, service);
                } catch (Exception e) {
                    throw new EpiException("Failed to instantiate service " + propName, e);
                }
            }
        }
        
        for (String serviceName : serviceObjects.keySet()) {
            Service service = serviceObjects.get(serviceName);
            Map<String, String> serviceConfig = configs.get(serviceName);
            if (serviceConfig != null) {
                service.init(serviceConfig, context);
            }
            services.put(serviceName, service);
        }
    }

    /**
     * Parse a component init string "class-name,p1=val1,p2=val2,..." and instantiate the service
     */
    private Service parseInit(String serviceName, String init, ServletContext context) {
        String[] segments = init.split(",");
        if (segments.length == 0) {
            throw new EpiException("No classname found for " + serviceName);
        }
        try {
            Service service = (Service) Class.forName( segments[0].trim() ).newInstance();
            Map<String, String> config = new HashMap<String, String>();
            for (int i = 1; i < segments.length; i++) {
                String param = segments[i].trim();
                String split[] = param.split("=");
                if (split.length != 2) {
                    throw new EpiException("Couldn't parse config parameter " + param);
                }
                config.put(split[0].trim(), split[1].trim());
            }
            service.init(config, context);
            return service;
        } catch (Exception e ) {
            throw new EpiException("Failed to instantiate service " + serviceName, e);
        }
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
//
//    private String withTrailingSlash(String path) {
//        return path.endsWith("/") ? path : path + "/";
//    }

    @Override
    public synchronized void contextDestroyed(ServletContextEvent sce) {
        for (String sname : services.keySet()) {
            Service service = services.get(sname);
            if (service instanceof Shutdown) {
                ((Shutdown)service).shutdown();
                log.info("Shut down " + sname);
            }
        }
    }

}
