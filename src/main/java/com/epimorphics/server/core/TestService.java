/******************************************************************
 * File:        Temp.java
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

import java.util.Map;

import javax.servlet.ServletContext;

public class TestService implements Service {

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        System.out.println("Test service init called");
        for (String conf : config.keySet()) {
            System.out.println(" " + conf + " = " + config.get(conf));
        }
    }

    @Override
    public void postInit() {
        System.out.println("Post init called");
    }

    @Override
    public void postPostInit() {
    }

}
