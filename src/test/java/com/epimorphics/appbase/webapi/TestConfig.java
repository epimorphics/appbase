/******************************************************************
 * File:        TestConfig.java
 * Created by:  Dave Reynolds
 * Created on:  22 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;


public class TestConfig extends TomcatTestBase {

    @Override
    String getWebappRoot() {
        return "src/test/basicWebapp";
    }
    
    @Test
    public void testConfig() {
        App app = AppConfig.get();
        assertEquals("This is a string", app.getParam("stringParam"));
        assertEquals(new Long(42), app.getParam("intParam"));
        
        TrialBean component1 = app.getComponentAs("component1", TrialBean.class);
        assertEquals("name 1", component1.getProp1());
        assertEquals(1, component1.getProplong());
        
        TrialBean component2 = app.getComponentAs("component2", TrialBean.class);
        assertEquals("name 2", component2.getProp1());
        assertEquals(component1, component2.getRef());
    }
}
