/******************************************************************
 * File:        TestDynamicLoad.java
 * Created by:  Dave Reynolds
 * Created on:  12 Apr 2017
 * 
 * (c) Copyright 2017, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.test.LoaderTest;

/**
 * Test dynamic class loading 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestDynamicLoad {
    App testapp;
    
    @Before
    public void startup() throws IOException {
        AppConfig.startApp("test", "src/test/loaderTestApp/app.conf");
        testapp = AppConfig.getApp();
        testapp.startup();
    }
    
    @After
    public void shutdown() {
        if (testapp != null) {
            testapp.shutdown();
        }
    }
    
    @Test
    public void testLoadOfTest() {
        LoaderTest t = testapp.getComponentAs("test1", LoaderTest.class);
        assertNotNull(t);
        assertEquals("Test saw message='foo', arg='I am Groot'", t.perform("foo"));
    }
}
