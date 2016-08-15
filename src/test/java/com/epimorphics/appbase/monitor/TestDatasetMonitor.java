/******************************************************************
 * File:        TestDatasetMonitor.java
 * Created by:  Dave Reynolds
 * Created on:  27 Apr 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.monitor;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.appbase.data.TestUnionSource;
import com.epimorphics.appbase.data.impl.UnionDatasetSparqlSource;
import com.epimorphics.appbase.monitor.DatasetMonitor.MonitoredGraph;
import com.epimorphics.util.FileUtil;
import com.epimorphics.util.TestUtil;
import org.apache.jena.query.ResultSet;
import org.apache.jena.util.FileUtils;

public class TestDatasetMonitor {
    protected App app;
    protected DatasetMonitor monitor;
    protected CachingDatasetMonitor cmonitor;
    protected SparqlSource source;
    protected File testDir;
    
    private static final int MONITOR_CHECK_DELAY = 15;
    private static final int NTRIES = 100;
    
    @Before
    public void setup() throws IOException {
        testDir = Files.createTempDirectory("testmonitor").toFile();
        app = new App("TestMonitor");
        
        source = new UnionDatasetSparqlSource();
        
        monitor = new DatasetMonitor();
        monitor.setDirectory(testDir.getPath());
        monitor.setFileSampleLength(1000);
        monitor.setSparqlSource(source);
        app.addComponent("monitor", monitor);
        
        cmonitor = new CachingDatasetMonitor();
        cmonitor.setDirectory(testDir.getPath());
        cmonitor.setFileSampleLength(1000);
        cmonitor.setSparqlSource(source);
        app.addComponent("cmonitor", cmonitor);
        
        app.startup();
    }
    
    @After
    public void cleanUp() {
        FileUtil.deleteDirectory(testDir);
    }

    @Test
    public void testMonitor() throws IOException, InterruptedException {
        monitor.setScanInterval(5);
        cmonitor.setScanInterval(5);
        assertTrue( monitor.getEntries().isEmpty() );
        
        addFile("graph1", "g1.ttl");
        waitFor(monitor, "file:g1.ttl", true);
        TestUtil.testArray(TestUnionSource.checkGraphs(source), new String[]{"graph1"});
        
        addFile("graph2", "subdir/g2.ttl");
        waitFor(monitor, "file:subdir/g2.ttl", true);
        TestUtil.testArray(TestUnionSource.checkGraphs(source), new String[]{"graph1", "graph2"});
        TestUtil.testArray(checkGraphNames(source), new String[]{"file:g1.ttl", "file:subdir/g2.ttl"});
        
        waitFor(cmonitor, "file:subdir/g2.ttl", true);
        assertEquals(2, cmonitor.getCachedUnion().size());
        
        removeFile("g1.ttl");
        waitFor(monitor, "file:g1.ttl", false);
        TestUtil.testArray(TestUnionSource.checkGraphs(source), new String[]{"graph2"});
        TestUtil.testArray(checkGraphNames(source), new String[]{"file:subdir/g2.ttl"});
        
        waitFor(cmonitor, "file:g1.ttl", false);
        assertEquals(1, cmonitor.getCachedUnion().size());
    }
    
    protected void waitFor(DatasetMonitor dsmon, String graphname, boolean present) throws InterruptedException {
        for (int t = 0; t < NTRIES; t++) {
            Thread.sleep(MONITOR_CHECK_DELAY);
            MonitoredGraph m = dsmon.get(graphname);
            if ( (present && m != null) || (!present && m == null) ) {
                return;
            }
        }
        assertTrue("Failed to detected " + (present ? "addition" : "removal") + " of " + graphname, false);
    }
    
    protected void addFile(String marker, String filename) throws IOException {
        File file = new File(testDir, filename);
        FileUtil.ensureDir( file.getParent() );
        FileOutputStream out = new FileOutputStream(file);
        TestUnionSource.createGraph(marker).write(out, FileUtils.langTurtle);
        out.close();
    }
    
    protected void removeFile(String filename) throws IOException {
        File file = new File(testDir, filename);
        file.delete();
    }
    
    public static List<String> checkGraphNames(SparqlSource source) {
        ResultSet rs = source.select("SELECT ?g WHERE {GRAPH ?g {}}");
        List<String> results = new ArrayList<>();
        while (rs.hasNext()) {
            results.add( rs.next().getResource("g").getURI() );
        }
        return results;
    }

}
