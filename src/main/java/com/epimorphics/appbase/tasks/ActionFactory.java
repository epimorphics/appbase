/******************************************************************
 * File:        ActionFactory.java
 * Created by:  Dave Reynolds
 * Created on:  18 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Facility to create an Action object from a configuration file.
 * Extensible by registering new factorylets.
 */
public class ActionFactory {
    public interface Factorylet {
        public boolean canConfigure(File file);
        public Collection<Action> configure(File file);
    }
    
    protected static List<Factorylet> factories = new ArrayList<>();
    
    public static Collection<Action> configure(File file) {
        for (Factorylet f : factories) {
            if (f.canConfigure(file)) {
                return f.configure(file);
            }
        } 
        return Collections.emptyList();
    }
    
    public static void register(Factorylet factory) {
        factories.add(factory);
    }
    
    static {
        register( new ActionJsonFactorylet() );
    }
}
