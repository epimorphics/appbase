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
import java.util.Collection;

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
    
    @Before
    public void setup() throws IOException {
        testDir = Files.createTempDirectory("testmonitor").toFile();
        app = new App("TestMonitor");
        monitor = new TMonitor();
        monitor.setDirectory(testDir.getPath());
        monitor.setFileSampleLength(1000);
        monitor.startup(app);
    }
    
    @After
    public void cleanUp() {
        FileUtil.deleteDirectory(testDir);
    }

    @Test
    public void testMonitor() throws IOException, InterruptedException {
        monitor.setScanInterval(5);
        assertTrue( monitor.getEntries().isEmpty() );
        
        touchFile("foo", "foo1");
        Thread.sleep(20);
        
        Collection<TestInstance> entries = monitor.getEntries();
        assertEquals(1, entries.size());
        
        TestInstance[] entryArray = new TestInstance[1];
        entryArray =  entries.toArray(entryArray);
        TestInstance fooInst = entryArray[0];
        assertEquals("foo1", fooInst.getMessage());
        
        touchFile("bar", "bar1");
        Thread.sleep(20);
        
        entries = monitor.getEntries();
        assertEquals(2, entries.size());
        TestInstance fooCheck = monitor.getLatest(fooInst);
        TestUtil.testArray(entryNames(entries), new String[]{"foo1", "bar1"});
        assertEquals(fooCheck, fooInst);
        
        touchFile("foo", "foo2");
        Thread.sleep(20);
        entries = monitor.getEntries();
        assertEquals(2, entries.size());
        TestUtil.testArray(entryNames(entries), new String[]{"foo2", "bar1"});
        fooCheck = monitor.getLatest(fooInst);
        assertNotSame(fooInst, fooCheck);
        assertEquals("foo2", fooCheck.getMessage());
        
        fooCheck.getSourceFile().delete();
        Thread.sleep(20);
        assertEquals(1, entries.size());
        TestUtil.testArray(entryNames(entries), new String[]{"bar1"});
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
        protected File sourceFile;
        protected String message;
        
        public TestInstance(String message) {
            this.message = message;
        }
        
        @Override
        public File getSourceFile() {
            return sourceFile;
        }
        @Override
        public void setSourceFile(File file) {
            this.sourceFile = file;
        }
        
        
        public void setMessage(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
    }
    
    public static class TMonitor extends ConfigMonitor<TestInstance> {
        @Override
        protected TestInstance configure(File file) {
            String content = FileManager.get().readWholeFileAsUTF8(file.getPath());
            TestInstance i = new TestInstance( content );
            return i;
        }
        
    }
}
