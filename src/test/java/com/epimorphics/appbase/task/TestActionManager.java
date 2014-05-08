/******************************************************************
 * File:        TestActionManager.java
 * Created by:  Dave Reynolds
 * Created on:  19 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.task;

import static org.junit.Assert.assertEquals;
import static com.epimorphics.json.JsonUtil.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.jena.atlas.json.JsonObject;
import org.junit.Test;

import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.ProgressMonitorReporter;

public class TestActionManager {

    @Test
    public void testSimpleActions() throws InterruptedException, ExecutionException {
        Action action = new DummyAction();
        ActionManager am = new ActionManager();
        am.register(action);
        
        ActionManager.ActionExecution ae1 = am.runAction(action, createParams("message=Test message,count=2"));
        ActionManager.ActionExecution ae2 = am.runAction(action, createParams("message=Test message long,count=50"));

        assertEquals(2, am.listActiveExecutions().size());
        
        ae1.waitForCompletion();
//        dumpState(ae1);
        List<ProgressMessage> messages = ae1.getMonitor().getMessages();
        assertEquals(4, messages.size());
        assertTrue(messages.get(messages.size() - 1).toString().endsWith("finished"));
        assertEquals(1, am.listActiveExecutions().size());
        assertTrue(ae1.getMonitor().succeeded());
        assertEquals("Message: Test message", getPath(ae1.getResult(), "result"));
        
        ae2.waitForCompletion();
//        dumpState(ae2);
        Thread.sleep(10);  // Allow ActionManager to see the timeout and update the action state list, more robust method?
        messages = ae2.getMonitor().getMessages();
        assertTrue(messages.size() < 50);
        assertTrue(messages.get(messages.size() - 1).toString().endsWith("timeout"));
        assertFalse(ae2.getMonitor().succeeded());
            
        assertEquals(0, am.listActiveExecutions().size());

        assertEquals(ae1, am.getExecution(ae1.getId()));
        assertEquals(ae2, am.getExecution(ae2.getId()));
    }
    
    public static void dumpState(ActionManager.ActionExecution ae) {
        ProgressMonitorReporter monitor = ae.getMonitor();
        for (ProgressMessage msg : monitor.getMessages()) {
            System.out.println(msg.toString());
        }
        System.out.println(monitor.toString());
        System.out.println("Duration: " + ae.getDuration());
    }
    
    public static JsonObject createParams(String bindings) {
        JsonObject params = new JsonObject();
        if (bindings.trim().isEmpty()) {
            return params;
        }
        for (String binding : bindings.split(",")) {
            String[] pair = binding.split("=");
            String key = pair[0].trim();
            String value = pair[1].trim();
            try {
                int vint = Integer.parseInt(value);
                params.put(key, vint);
            } catch (NumberFormatException e) {
                params.put(key, value);
            }
        }
        return params;
    }
    
}
