/******************************************************************
 * File:        ReportAppConfig.java
 * Created by:  Dave Reynolds
 * Created on:  22 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.Startup;

public class ReportAppConfig implements Startup {

    @Override
    public void startup(App app) {
        System.out.println("App " + app.getName() + " configuration parameters");
        for (String p : app.listParams()) {
            System.out.println("    " + p + " = " + app.getParam(p));
        }
        
        System.out.println("Components:");
        for (String c : app.listComponentNames()) {
            System.out.println("    " + c + " = " + app.getComponent(c));
        }
    }
    
}