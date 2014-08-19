/******************************************************************
 * File:        ProcessingHook.java
 * Created by:  Dave Reynolds
 * Created on:  19 Aug 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

public interface ProcessingHook {

    public enum Event { Start, Complete, Error };
    
    public Event runOn();
    
    public void run(ActionExecution execution);
}
