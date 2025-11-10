/******************************************************************
 * File:        Temp.java
 * Created by:  Dave Reynolds
 * Created on:  25 Jul 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import org.apache.jena.atlas.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.epimorphics.json.JsonUtil;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;

// Directory monitor is deprecated and this tests does not work under java8
@Disabled
public class TestFileMonitor {
    
    protected File triggeredFile;
    protected JsonObject triggeredParameters;
    protected File testDir;

    @BeforeEach
    public void setup() throws IOException {
        testDir =  Files.createTempDirectory("watchtest").toFile();
        FileUtil.ensureDir(testDir.getPath());
        ConfigWatcher.start();
    }
    
    @AfterEach
    public void tearDown() {
        ConfigWatcher.stop();
        FileUtil.deleteDirectory(testDir);
    }
    
    @Test
    public void testMonitor() throws InterruptedException {
        DirectoryMonitor monitor = new TestMonitor();
        monitor.addWatch(testDir.getPath(), JsonUtil.makeJson(DirectoryMonitor.WAIT_TIME_PARAM, 100, "test", "foobar"));
        File testFile = new File(testDir, "test1"); 
        
        Thread t = new Thread(new FileGenerator(testFile.getPath(), 5, 10));
        t.start();
        t.join();
        
        int MAX_WAIT = 10;
        for (int i = 0; i < MAX_WAIT; i++) {
            Thread.sleep(1000);
            synchronized (this) {
                if ( triggeredFile != null) break;
            }
        }
        
        assertNotNull( triggeredFile );
        assertEquals( testFile, triggeredFile );
        assertEquals( "foobar", JsonUtil.getStringValue(triggeredParameters, "test") );
    }
    
    public class TestMonitor extends DirectoryMonitor {

        @Override
        protected synchronized void action(File file, JsonObject parameters) {
            triggeredFile = file;
            triggeredParameters = parameters;
        }
        
    }
    
    public final class FileGenerator implements Runnable {
        String filename;
        int size;
        int waitTime;
        
        public FileGenerator(String filename, int size, int waitTime) {
            this.filename = filename;
            this.size = size;
            this.waitTime = waitTime;
        }
        
        @Override
        public void run() {
            File file = new File(filename);
            try {
                PrintStream out = new PrintStream(file);
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < 500; j++) {
                        out.print("This is a test ");
                    }
                    out.print("\n");
                    out.flush();
                    Thread.sleep(waitTime);
                }
                out.close();
            } catch (IOException e) {
                throw new EpiException(e);
            } catch (InterruptedException e) {
                return;
            }
        }
        
    }
}
