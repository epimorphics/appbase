/******************************************************************
 * File:        ServiceBase.java
 * Created by:  Dave Reynolds
 * Created on:  3 Dec 2012
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

import java.util.Map;

import javax.servlet.ServletContext;

import com.epimorphics.util.EpiException;

public class ServiceBase implements Service {

    protected Map<String, String> config;

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        this.config = config;
    }

    @Override
    public void postInit() {
    }

    protected String getRequiredParam(String param) {
        String location = config.get(param);
        if (location == null) {
            throw new EpiException("Missing requried configuration parameter: " + param);
        }
        return location;
    }
    
    protected int getRequiredIntParam(String param) {
        String val = getRequiredParam(param);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new EpiException("Expected " + param + " to be a integer", e);
        }
    }

    protected String getFileParam(String param) {
        String location = config.get(param);
        if (location == null){
            return null;
        } else {
            return ServiceConfig.get().expandFileLocation( location);
        }
    }

    protected String getRequiredFileParam(String param) {
        return ServiceConfig.get().expandFileLocation( getRequiredParam(param)) ;
    }
    
    @SuppressWarnings("unchecked")
    protected <T> T getNamedService(String name, Class<T> type) {
        Object service = ServiceConfig.get().getService(name);
        if (type.isInstance(service)) {
            return (T)service;
        } else {
            throw new EpiException("Service " + name + " was not of expected type (" + type + ")"); 
        }
    }

    @Override
    public void postPostInit() {
    }

}
