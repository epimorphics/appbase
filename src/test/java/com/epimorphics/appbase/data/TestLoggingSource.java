/******************************************************************
 * File:        TestLoggingSource.java
 * Created by:  Dave Reynolds
 * Created on:  3 Feb 2017
 * 
 * (c) Copyright 2017, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.impl.DatasetSparqlSource;
import com.epimorphics.appbase.data.impl.LoggingSparqlSource;

/**
 * Test support for logging 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestLoggingSource {
    protected static final String TEST = "http://localhost/test/def#";
    
    App app;
    SparqlSource source;
    LoggingSparqlSource lsource;
    File logDir;
    
    @Before
    public void setup() throws IOException {
        Path path = Files.createTempDirectory("loggingSourceTest");
        logDir = path.toFile();
        logDir.deleteOnExit();
        
        app = new App("test");
        source = new DatasetSparqlSource();
        app.addComponent("source", source);
        
        lsource = new LoggingSparqlSource();
        lsource.setSource(source);
        lsource.setLogDirectory( path.toString() );
        app.addComponent("lsource", lsource);
        
        app.startup();
    }
    
    @Test
    public void testLoggingSource() {
        DatasetAccessor accessor = lsource.getAccessor();
        accessor.add(TEST + "g1", createGraph("g1"));
        accessor.add(createGraph("default"));
        accessor.putModel(TEST + "g1", createGraph("g1b"));
        accessor.deleteModel(TEST + "g1");
        lsource.update( UpdateFactory.create( String.format("INSERT DATA { <%s> <%s> 'u1' }", TEST+"u", RDFS.label.getURI()) ));
        compareDirectories(new File("src/test/data/logging-expected"), logDir);
    }
    
    public static void compareDirectories(File expectedDir, File actualDir) {
        String[] expectedFiles = expectedDir.list();
        Arrays.sort(expectedFiles);
        String[] actualFiles = actualDir.list();
        Arrays.sort(actualFiles);
        assertEquals(expectedFiles.length, actualFiles.length);
        for (int i = 0; i < expectedFiles.length; i++) {
            String expected = FileManager.get().readWholeFileAsUTF8( new File(expectedDir, expectedFiles[i]).getPath() );
            String actual = FileManager.get().readWholeFileAsUTF8( new File(actualDir, actualFiles[i]).getPath() );
            assertEquals(expected, actual);
        }
    }
    
    public static Model createGraph(String marker) {
        Model m = ModelFactory.createDefaultModel();
        m.createResource(TEST + "i")
            .addProperty(RDFS.label, marker);
        return m;
    }
    
}
