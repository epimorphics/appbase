/******************************************************************
 * File:        TestLib.java
 * Created by:  Dave Reynolds
 * Created on:  15 May 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
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

package com.epimorphics.server.templates;

import java.util.Calendar;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import static org.junit.Assert.*;

public class TestLib {

    Lib lib = new Lib();
    
    @Test
    public void testIsDateTime() {
        Model m = ModelFactory.createDefaultModel();
        assertFalse(lib.isDatetime( m.createLiteral("fool")));
        assertFalse(lib.isDatetime( m.createTypedLiteral(42)));
        assertTrue(lib.isDatetime( m.createTypedLiteral( Calendar.getInstance() )));
    }
}
