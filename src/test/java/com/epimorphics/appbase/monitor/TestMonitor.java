/******************************************************************
 * File:        TestMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  19 Jan 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.util.FileUtil;
import com.epimorphics.util.TestUtil;
import com.hp.hpl.jena.util.FileManager;

public class TestMonitor {
    protected App app;
    protected TMonitor monitor;
    protected File testDir;
    protected File fubarFile;
    
//    private static final int MONITOR_CHECK_DELAY = 1000;
//    private static final int NTRIES = 60;
    private static final int MONITOR_CHECK_DELAY = 15;
    private static final int NTRIES = 20;
    
    @Before
    public void setup() throws IOException {
        testDir = Files.createTempDirectory("testmonitor").toFile();
        fubarFile = touchFile("fubar", "fubar1");
        
        app = new App("TestMonitor");
    }
    
    @After
    public void cleanUp() {
        FileUtil.deleteDirectory(testDir);
    }
    
    @Test
    public void testScanner() throws IOException, InterruptedException {
        monitor = new TMonitor();
        monitor.setDirectory(testDir.getPath());
        monitor.setFileSampleLength(1000);
        monitor.setScanInterval(5);

        app.addComponent("monitor", monitor);
        app.startup();

        doTestMonitor();
    }
    
    @Test
    public void testWatcher() throws IOException, InterruptedException {
        
        monitor = new TMonitor();
        monitor.setUseWatcher(true);
        monitor.setDirectory(testDir.getPath());

        app.addComponent("monitor", monitor);
        app.startup();

        doTestMonitor();
    }

    public void doTestMonitor() throws IOException, InterruptedException {
        waitFor("fubar", true);

        Collection<TestInstance> entries = monitor.getEntries();
        assertEquals(1, entries.size());

        assertTrue( fubarFile.delete() );
        waitFor("fubar", false);
        assertTrue( monitor.getEntries().isEmpty() );
        
        File fooFile = touchFile("foo", "foo1");
        waitFor("foo", true);
        
        entries = monitor.getEntries();
        assertEquals(1, entries.size());
        
        TestInstance[] entryArray = new TestInstance[1];
        entryArray =  entries.toArray(entryArray);
        TestInstance fooInst = entryArray[0];
        assertEquals("foo1", fooInst.getMessage());
        
        touchFile("bar", "bar1");
        waitFor("bar", true);
        
        entries = monitor.getEntries();
        assertEquals(2, entries.size());
        TestInstance fooCheck = monitor.get("foo");
        TestUtil.testArray(entryNames(entries), new String[]{"foo1", "bar1"});
        assertEquals(fooCheck, fooInst);
        
        touchFile("foo", "foo2");
        Thread.sleep(MONITOR_CHECK_DELAY * NTRIES);  // Not sure how better wait for change detection
        entries = monitor.getEntries();
        assertEquals(2, entries.size());
        TestUtil.testArray(entryNames(entries), new String[]{"foo2", "bar1"});
        fooCheck = monitor.get("foo");
        assertNotSame(fooInst, fooCheck);
        assertEquals("foo2", fooCheck.getMessage());
        
        fooFile.delete();
        waitFor("foo", false);
        entries = monitor.getEntries();
        assertEquals(1, entries.size());
        TestUtil.testArray(entryNames(entries), new String[]{"bar1"});
        
        File dir = FileUtil.ensureDir(testDir + "/dir");
        File childF = touchFile("dir/child", "child1");
        waitFor("child", true);
        
        childF.delete();
        dir.delete();
        waitFor("child", false);
    }
    
    private File touchFile(String file, String content) throws IOException {
        File f = new File(testDir + "/" + file);
        FileWriter w = new FileWriter(f, false);
        w.append(content);
        w.close();
        return f;
    }

    private String[] entryNames(Collection<TestInstance> entries) {
        String[] results = new String[ entries.size() ];
        int i = 0;
        for (TestInstance ti : entries) {
            results[i++] = ti.getMessage();
        }
        return results;
    }
    
    public static class TestInstance implements ConfigInstance {
        protected String name;
        protected String message;
        
        public TestInstance(String message) {
            this.message = message;
        }
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setMessage(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
    }
    
    protected void waitFor(String file, boolean present) throws InterruptedException {
        for (int t = 0; t < NTRIES; t++) {
            Thread.sleep(MONITOR_CHECK_DELAY);
            TestInstance m = monitor.get(file);
            if ( (present && m != null) || (!present && m == null) ) {
                return;
            }
        }
        assertTrue("Failed to detected " + (present ? "addition" : "removal") + " of " + file, false);
    }
    
    public static class TMonitor extends ConfigMonitor<TestInstance> {
        @Override
        protected Collection<TestInstance> configure(File file) {
            String content = FileManager.get().readWholeFileAsUTF8(file.getPath());
            TestInstance i = new TestInstance( content );
            i.setName( file.getName() );
            List<TestInstance> results = new ArrayList<>();
            results.add(i);
            return results;
        }
        
    }
}
