/******************************************************************
 * File:        ExampleLibPlugin.java
 * Created by:  Dave Reynolds
 * Created on:  23 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.templates.LibPlugin;

public class ExampleLibPlugin extends ComponentBase implements LibPlugin {
    
    public String run() {
        return "Hello from lib plugin - " + getName() + " in application " + app.getName();
    }

}
