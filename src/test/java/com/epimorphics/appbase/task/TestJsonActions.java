/******************************************************************
 * File:        TestJsonActions.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import static com.epimorphics.appbase.task.TestActionManager.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.appbase.tasks.ActionManager.ActionExecution;
import com.epimorphics.tasks.ProgressMessage;
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
    
    @Test
    public void testErrorHander() throws InterruptedException {
        ActionExecution ae = runAction("testErrorHandler", "");
        ProgressMonitorReporter pm = ae.getMonitor();
        assertFalse(pm.succeeded());
        assertEquals(4, pm.getMessages().size());
        assertTrue( pm.getMessages().get(0).getMessage().contains("Forcing error from CreateErrorAction") );
        assertEquals( "Error detected", pm.getMessages().get(2).getMessage() );

        ae = runAction("testErrorTimeout", "");
        Thread.sleep(10);  // Allow time out processing to complete, more robust way?
        pm = ae.getMonitor();
        assertFalse(pm.succeeded());
        List<ProgressMessage> messages = pm.getMessages();
        assertEquals( "Timeout detected", messages.get(messages.size() - 2).getMessage());
    }
    
    private ActionExecution runAction(String actionName, String args) {
        assertNotNull( am.get(actionName) );
        ActionExecution ae = am.runAction(actionName, createParams(args));
        ae.waitForCompletion();
        return ae;
    }

}
