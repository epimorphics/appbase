/******************************************************************
 * File:        JavaAction.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import com.epimorphics.appbase.tasks.Action;

/**
 * Action that calls a java class with some additional configuration 
 * parameters bound in.
 */
public class JavaAction extends WrappedAction implements Action {

    public void setAction(String classname) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        baseAction = (Action)Class.forName(classname).newInstance();
        mergeBaseConfiguration(baseAction);
    }

}
