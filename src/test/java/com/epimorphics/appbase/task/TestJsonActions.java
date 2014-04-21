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
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
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
    
    @After
    public void shutdown() {
        testapp.shutdown();
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
//        dumpState(ae);
        ProgressMonitorReporter pm = ae.getMonitor();
        assertFalse(pm.succeeded());
        assertEquals(2, pm.getMessages().size());
        assertTrue( pm.getMessages().get(0).getMessage().contains("Forcing error from CreateErrorAction") );
        assertEquals( "Error detected", pm.getMessages().get(1).getMessage() );

        ae = runAction("testErrorTimeout", "");
        Thread.sleep(10);  // Allow time out processing to complete, more robust way?
        pm = ae.getMonitor();
        assertFalse(pm.succeeded());
        List<ProgressMessage> messages = pm.getMessages();
        assertEquals( "Timeout detected", messages.get(messages.size() - 1).getMessage());
    }
    
    @Test
    public void testCompoundflows() throws InterruptedException {
        ActionExecution ae = runAction("sequenceTest", "");
        ProgressMonitorReporter pm = ae.getMonitor();
        assertTrue(pm.succeeded());
        List<ProgressMessage> messages = pm.getMessages();
        assertEquals(3, messages.size());
        assertEquals("sequence 1", messages.get(0).getMessage());
        assertEquals("sequence 2", messages.get(1).getMessage());
        assertEquals("sequence 3", messages.get(2).getMessage());

        ae = runAction("parTest", "");
//        dumpState(ae);
        pm = ae.getMonitor();
        assertTrue(pm.succeeded());
        messages = pm.getMessages();
        assertEquals(3, messages.size());
        for (int i = 0; i < 3; i++) {
            assertTrue( messages.get(i).getMessage().matches("par [123]") );
        }
    }
    
    @Test
    public void testEvents() {
        RecordingAction.reset();
        List<ActionExecution> aes = new ArrayList<>();
        aes.addAll( am.fireEvent("test/foo", createParams("")) );
        aes.addAll( am.fireEvent("miss/foo", createParams("")) );
        aes.addAll( am.fireEvent("test/bar", createParams("")) );
        for (ActionExecution ae : aes) {
            ae.waitForCompletion();
        }
        List<String> firings = RecordingAction.getMessages();
        assertEquals(2, firings.size());
        assertTrue( 
                (firings.get(0).equals("fired - test/foo") && firings.get(1).equals("fired - test/bar")) 
                ||
                (firings.get(0).equals("fired - test/bar") && firings.get(1).equals("fired - test/foo")) 
                );
        RecordingAction.reset();
    }
    
    private ActionExecution runAction(String actionName, String args) {
        assertNotNull( am.get(actionName) );
        ActionExecution ae = am.runAction(actionName, createParams(args));
        ae.waitForCompletion();
        return ae;
    }

}
