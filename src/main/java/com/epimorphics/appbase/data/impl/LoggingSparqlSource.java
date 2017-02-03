/******************************************************************
 * File:        LoggingSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  2 Feb 2017
 * 
 * (c) Copyright 2017, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.update.UpdateRequest;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.data.ClosableResultSet;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;

/**
 * Wraps a sparql source with support for logging all updates
 * to a directory of update files.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LoggingSparqlSource extends ComponentBase implements SparqlSource {
    public static final String OP_PUT = "PUT";
    public static final String OP_DELETE = "DELETE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_ADD = "ADD";
    public static final String DEFAULT_MODEL = "default";
    
    protected SparqlSource source;
    protected File logDir;
    
    protected long counter = 0;
    
    public void setSource(SparqlSource source) {
        this.source = source;
    }
    
    public void setLogDirectory(String dir) {
        this.logDir = new File(dir);
        FileUtil.ensureDir(dir);
    }
    
    // The logging operations
    protected OutputStream startLogEntry(String op, String model, String format) throws IOException {
        String logname = String.format( "%s-%d-%s.%s",
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-S").format(new Date()), counter++, op , format);
        File logfile = new File(logDir, logname);
        BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(logfile) );
        String header = String.format("# %s %s\n", op, model);
        out.write( header.getBytes(StandardCharsets.UTF_8) );
        return out;
    }
    
    public void logEntry(String op, String modelID) {
        try {
            OutputStream out = startLogEntry(op, modelID, "log");
            out.close();
        } catch (IOException e) {
            throw new EpiException("Failed to create log entry", e);
        }
    }
    
    public void logEntry(String op, String modelID, Model model) {
        try (
            OutputStream out = startLogEntry(op, modelID, "ttl");
        ) {
            model.write(out, "Turtle");
        } catch (IOException e) {
            throw new EpiException("Failed to create log entry", e);
        }
    }
    
    public void logUpdate(UpdateRequest request) {
        try (
            OutputStream out = startLogEntry(OP_UPDATE, "default", "ru");
        ) {
            request.output( new IndentedWriter(out) );
        } catch (IOException e) {
            throw new EpiException("Failed to create log entry", e);
        }
    }

    @Override
    public ResultSet select(String query) {
        return source.select(query);
    }

    @Override
    public ClosableResultSet streamableSelect(String query) {
        return source.streamableSelect(query);
    }

    @Override
    public <T> List<T> selectVar(String query, String varname, Class<T> cls) {
        return selectVar(query, varname, cls);
    }

    @Override
    public Graph describe(String query) {
        return source.describe(query);
    }

    @Override
    public Graph describeAll(String... uris) {
        return source.describeAll(uris);
    }

    @Override
    public Graph[] describeEach(String... uris) {
        return source.describeEach(uris);
    }

    @Override
    public Graph construct(String query) {
        return source.construct(query);
    }

    @Override
    public boolean ask(String query) {
        return source.ask(query);

    }

    @Override
    public void update(UpdateRequest update) {
        source.update(update);
        logUpdate(update);
    }

    @Override
    public boolean isUpdateable() {
        return source.isUpdateable();
    }

    @Override
    public DatasetAccessor getAccessor() {
        return new LoggingAccessor();
    }

    @Override
    public PrefixMapping getPrefixes() {
        return source.getPrefixes();
    }

    public class LoggingAccessor implements DatasetAccessor {

        @Override
        public Model getModel() {
            return source.getAccessor().getModel();
        }

        @Override
        public Model getModel(String graphUri) {
            return source.getAccessor().getModel(graphUri);
        }

        @Override
        public boolean containsModel(String graphURI) {
            return source.getAccessor().containsModel(graphURI);
        }

        @Override
        public void putModel(Model data) {
            source.getAccessor().putModel(data);
            logEntry(OP_PUT, DEFAULT_MODEL, data);
        }

        @Override
        public void putModel(String graphUri, Model data) {
            source.getAccessor().putModel(graphUri, data);
            logEntry(OP_PUT, graphUri, data);
        }

        @Override
        public void deleteDefault() {
            source.getAccessor().deleteDefault();
            logEntry(OP_DELETE, DEFAULT_MODEL);
        }

        @Override
        public void deleteModel(String graphUri) {
            source.getAccessor().deleteModel(graphUri);
            logEntry(OP_DELETE, graphUri);
        }

        @Override
        public void add(Model data) {
            source.getAccessor().add(data);
            logEntry(OP_ADD, DEFAULT_MODEL, data);
        }

        @Override
        public void add(String graphUri, Model data) {
            source.getAccessor().add(graphUri, data);
            logEntry(OP_ADD, graphUri, data);
        }
        
    }
    
}
