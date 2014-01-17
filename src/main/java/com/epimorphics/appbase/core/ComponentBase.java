/******************************************************************
 * File:        ComponentBase.java
 * Created by:  Dave Reynolds
 * Created on:  22 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Optional base class for components. A component can be any Java Bean
 * so this base class is not required. Provides local pointer to the
 * app which contains this component.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ComponentBase implements Startup, Named {
    static Logger log = LoggerFactory.getLogger(ComponentBase.class);
            
    protected App app;
    protected String componentName;
    
    @Override
    public void startup(App app) {
        this.app = app;
    }

    public static File asFile(String filename) {
        return new File( expandFileLocation(filename) );
    }

    public static String expandFileLocation(String filename) {
        return AppConfig.getAppConfig().expandFileLocation(filename);
    }
    
    public void require(Object value, String valuename) {
        if (value == null) {
            log.error(String.format("Missing parameter %s on component %s.%s", valuename, app.getName(), componentName));
        }
    }

    @Override
    public String getName() {
        return componentName;
    }

    @Override
    public void setName(String name) {
        componentName = name;
    }
    
    public App getApp() {
        return app;
    }
    
}
