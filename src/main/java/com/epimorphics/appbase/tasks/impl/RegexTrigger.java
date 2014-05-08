/******************************************************************
 * File:        RegexTrigger.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks.impl;

import java.util.regex.Pattern;

import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.tasks.ActionTrigger;

/**
 * Simple trigger which matches the event name against a regex,
 * no indexing supported.
 */
public class RegexTrigger implements ActionTrigger {
    protected Pattern matcher;
    
    public RegexTrigger(String pattern) {
        matcher = Pattern.compile(pattern);
    }
    
    @Override
    public boolean matches(String trigger, JsonObject parameters) {
        return matcher.matcher(trigger).matches();
    }

}
