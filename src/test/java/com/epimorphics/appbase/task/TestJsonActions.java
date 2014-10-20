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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionExecution;
import com.epimorphics.appbase.tasks.ActionInstance;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.appbase.tasks.ProcessingHook;
import com.epimorphics.appbase.tasks.ProcessingHook.Event;
import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.ProgressMonitor;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.tasks.SimpleProgressMonitor;
import com.epimorphics.util.FileUtil;

public class TestJsonActions {
    App testapp;
    ActionManager am;
    File testDir;
    
    @Before
    public void startup() throws IOException {
        testapp = new App("testapp", new File("src/test/actionApp/app.conf"));
        AppConfig.theConfig.setDefaultApp(testapp);
        am = testapp.getComponentAs("actionManager", ActionManager.class);
        am.setMaxHistory(1);
        testDir = Files.createTempDirectory("testmonitor").toFile();
        am.setTraceDir( testDir.getPath() );
        testapp.startup();
    }
    
    @After
    public void shutdown() {
        if (testapp != null) {
            testapp.shutdown();
        }
        FileUtil.deleteDirectory(testDir);
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
        
        ActionExecution restored = am.getExecution( ae.getId() );
        assertEquals( "messageThrice", restored.getAction().getName() );
        assertEquals( ae.getDuration(), restored.getDuration() );
        assertEquals( 5, restored.getMonitor().getMessages().size() );
    }
    
    @Test
    public void testNaming() {
        ActionExecution ae = runAction("messageThrice", "message=test");
        ProgressMonitorReporter pm = ae.getMonitor();
        assertTrue(pm.succeeded());
        
        assertEquals("messageThrice", ae.getAction().getName());
        ae.setName("My run");
        assertEquals("My run", ae.getName());
        assertEquals("My run", ae.getActionInstance().getName());
        assertEquals("messageThrice", ae.getAction().getName());
        
        ActionInstance inst = am.makeInstance("messageThrice", "message", "test 2");
        inst.setName("My run 2");
        ae = inst.start();
        ae.waitForCompletion();
        assertEquals("messageThrice", ae.getAction().getName());
        assertEquals("My run 2", ae.getName());

    }
    
    @Test
    public void testErrorHandler() throws InterruptedException {
        ActionExecution ae = runAction("testErrorHandler", "");
        ProgressMonitorReporter pm = ae.getMonitor();
        assertFalse(pm.succeeded());
        assertEquals(2, pm.getMessages().size());
        assertTrue( pm.getMessages().get(0).getMessage().contains("Forcing error from CreateErrorAction") );
        assertEquals( "Error detected", pm.getMessages().get(1).getMessage() );

        // TODO debug why this test is unreliable outside of eclipse on fast machine
        /*
        ae = runAction("testErrorTimeout", "");
        pm = ae.getMonitor();
        assertFalse(pm.succeeded());
        List<ProgressMessage> messages = pm.getMessages();
        int n = messages.size();
//        System.out.println("Messages = " + messages);
        assertTrue( "Timeout detected".equals( messages.get(n-1).getMessage() ) 
                 || "Timeout detected".equals( messages.get(n-2).getMessage() ) );
        */
    }
    
    @Test
    public void testHooks() {
        TestHook startHook = new TestHook(Event.Start);
        am.installHook(startHook);
        TestHook endHook = new TestHook(Event.Complete);
        am.installHook(endHook);
        TestHook errorHook = new TestHook(Event.Error);
        am.installHook(errorHook);
        
        ActionExecution ae = runAction("messageThrice", "message=test");
        ae.waitForCompletion();
        assertEquals(ae.getId(), startHook.lastAEID());
        assertEquals(ae.getId(), endHook.lastAEID());
        assertNull( errorHook.lastAEID() );
        
        ae = runAction("testErrorHandler", "");
        ae.waitForCompletion();
        assertEquals(ae.getId(), startHook.lastAEID());
        assertEquals(ae.getId(), endHook.lastAEID());
        assertEquals(ae.getId(), errorHook.lastAEID());
    }
    
    class TestHook implements ProcessingHook {
        protected Event event;
        protected String lastAEID;
        
        public TestHook(Event event) {  this.event = event;    }
        public Event runOn() {  return event;      }
        public void run(ActionExecution execution) {
            lastAEID = execution.getId();
        }
        public String lastAEID() {
            return lastAEID;
        }
    }
    
    @Test
    public void testSuccessHander() throws InterruptedException {
        ActionExecution ae = runAction("testSuccessChain", "");
        ProgressMonitorReporter pm = ae.getMonitor();
        assertTrue(pm.succeeded());
        for (ProgressMessage message : pm.getMessages()){
            System.out.println(message.toString());
        }
        assertEquals(2, pm.getMessages().size());
        assertEquals( "Success action called", pm.getMessages().get(1).getMessage() );
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
    
    @Test
    public void testScripts() {
        // Explicit argument passing case
        ActionExecution ae = runAction("helloScriptArgs", "arg2=arg two");
        ProgressMonitor monitor = ae.getMonitor();
        assertTrue(monitor.succeeded());
        assertEquals("Hello from script: arg one arg two", monitor.getMessages().get(1).getMessage());
        assertEquals("Lib called", monitor.getMessages().get(2).getMessage());
        
        // Inline json case
        ae = runAction("helloJsonScript", "");
        monitor = ae.getMonitor();
        assertTrue(monitor.succeeded());
        assertEquals("Hello from script: {", monitor.getMessages().get(1).getMessage().trim());
        // Relies on json formatting
        assertTrue( monitor.getMessages().get(2).getMessage().contains("\"@name\" : \"helloJsonScript\"") );

        // Serialized json case
        ae = runAction("helloJsonRefScript", "");
        monitor = ae.getMonitor();
        assertTrue(monitor.succeeded());
        assertEquals("{", monitor.getMessages().get(1).getMessage().trim());
        // Relies on json formatting
        assertTrue( monitor.getMessages().get(2).getMessage().contains("\"@name\" : \"helloJsonRefScript\"") );
        
        // Failure detection
        ae = runAction("failScript", "");
        monitor = ae.getMonitor();
        assertFalse(monitor.succeeded());
        
        // Environment case
        ae = runAction("helloScriptEnv", "");
        monitor = ae.getMonitor();
        assertTrue(monitor.succeeded());
        assertEquals("Hello arg one, env foo=fubar", monitor.getMessages().get(1).getMessage());
        
        // Stderror case
        ae = runAction("stderr", "");
        monitor = ae.getMonitor();
        assertTrue(monitor.succeeded());
        assertEquals(6, monitor.getMessages().size());
        assertNotNull( findMessage("Hello on stdout", monitor) );
        ProgressMessage err = findMessage("Hello on stderr", monitor);
        assertNotNull(err);
        assertEquals("error", err.getType());
    }
    
    private ProgressMessage findMessage(String expected, ProgressMonitor monitor) {
        List<ProgressMessage> messages = monitor.getMessages();
        for (ProgressMessage message : messages) {
            if (message.getMessage().equals(expected)) {
                return message;
            }
        }
        return null;
    }

    @Test
    public void testInstance() {
        ActionInstance instance = am.makeInstance(new PrintAction(), "message", "hello");
        
        ActionExecution ae = instance.start();
        ae.waitForCompletion();
        checkMessages( ae.getMonitor(), "hello" );
        
        ProgressMonitorReporter monitor = new SimpleProgressMonitor();
        instance.run(monitor);
        checkMessages(monitor, "hello");
        
        instance.addAndThen( am.get("mark1") );

        ae = instance.start();
        ae.waitForCompletion();
        checkMessages( ae.getMonitor(), "hello", "mark1 called" );
        
        monitor = new SimpleProgressMonitor();
        instance.run(monitor);
        checkMessages( monitor, "hello", "mark1 called" );
    }
    
    private ActionExecution runAction(String actionName, String args) {
        Action a = am.get(actionName);
        assertNotNull( a ); 
        ActionExecution ae = am.runAction(actionName, createParams(args));
        ae.waitForCompletion();
        return ae;
    }
    
    
    private void checkMessages(ProgressMonitorReporter monitor, String...expected) {
        List<ProgressMessage> messages = monitor.getMessages();
        assertEquals(expected.length, messages.size());
        for (int i = 0; i < expected.length; i++) {
            assertTrue( messages.get(i).getMessage().startsWith( expected[i] ) );
        }
    }

}
