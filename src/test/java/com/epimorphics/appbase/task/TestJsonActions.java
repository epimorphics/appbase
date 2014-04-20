/******************************************************************
 * File:        TestJsonActions.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import static com.epimorphics.appbase.task.TestActionManager.createParams;
import static com.epimorphics.appbase.task.TestActionManager.dumpState;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.appbase.tasks.ActionManager.ActionExecution;
import com.epimorphics.tasks.ProgressMonitorReporter;

public class TestJsonActions {
    App testapp;
    ActionManager am;
    
    @Before
    public void startup() throws IOException {
        testapp = new App("testapp", new File("src/test/actionApp/app.conf"));
        AppConfig.theConfig.setDefaultApp(testapp);
        am = testapp.getComponentAs("actionManager", ActionManager.class);
        testapp.startup();
    }
    
    @Test
    public void testSimpleActions() {
        ActionExecution ae = runAction("messageThrice", "message=test");
        ProgressMonitorReporter pm = ae.getMonitor();
        assertTrue(pm.succeeded());
        assertEquals(5, pm.getMessages().size());
        assertTrue( pm.getMessages().get(0).getMessage().startsWith("messageThrice") );
        assertEquals( "test", pm.getMessages().get(1).getMessage() );
        
        pm = runAction("helloThrice", "").getMonitor();
        assertTrue(pm.succeeded());
        assertEquals(5, pm.getMessages().size());
        assertTrue( pm.getMessages().get(0).getMessage().startsWith("helloThrice") );
        assertEquals( "Hello", pm.getMessages().get(1).getMessage() );
    }
    
    private ActionExecution runAction(String actionName, String args) {
        assertNotNull( am.get(actionName) );
        ActionExecution ae = am.runAction(actionName, createParams(args));
        ae.waitForCompletion();
        return ae;
    }

}
