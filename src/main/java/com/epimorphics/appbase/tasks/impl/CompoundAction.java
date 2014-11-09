/******************************************************************
 * File:        CompoundAction.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.util.EpiException;

/**
 * Base class for actions which in turn invoke multiple other actions.
 */
public abstract class CompoundAction extends BaseAction {
    protected List<Object> components = new ArrayList<>();
    protected Action[] componentActions;
    
    public void addComponent(Object component) {
        components.add(component);
    }
    
    @Override
    public void resolve(ActionManager am) {
        super.resolve(am);
        componentActions = new Action[ components.size() ];
        for (int i = 0; i < components.size(); i++) {
            Action a = resolveAction(am, components.get(i));
            if (a != null) {
                componentActions[i] = a;
            } else {
                throw new EpiException("Could not resolve component on compound action: " + components.get(i));
            }
        }
    }

}
