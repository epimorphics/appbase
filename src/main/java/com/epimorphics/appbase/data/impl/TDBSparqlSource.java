/******************************************************************
 * File:        TDBSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  29 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDFS;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.data.SparqlSource;
import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.appbase.util.TimeStamp;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;

/**
 * A Sparql source which provides access to a TDB-based persistent
 * data store with an optional jena-text index.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TDBSparqlSource extends BaseSparqlSource implements SparqlSource {
    static Logger log = LoggerFactory.getLogger( TDBSparqlSource.class );
    
    protected File tdbDir;
    protected File textIndex;      
    protected File backupDir;
    protected String indexSpec = null;
    protected Dataset dataset;  // TODO should this be a thread local?
    protected boolean isUnionDefault;
    protected DatasetGraph graphStore;
    protected DatasetAccessor accessor;
    
    public void setLocation(String loc) {
        FileUtil.ensureDir(loc);
        tdbDir = asFile(loc);
        // Allow TDB to create the directory if it doesn't exist
//        if (!tdbDir.exists() || !tdbDir.canRead()) {
//            throw new EpiException("Configured location for TDB source is not accessible: " + loc);
//        }
    }
    
    public void setBackupDir(String loc) {
        FileUtil.ensureDir(loc);
        backupDir = asFile(loc);
    }
    
    /**
     * Set a directory from which the text index can be obtained
     */
    public void setIndex(String index) {
        textIndex = asFile(index);
    }

    /**
     * Configuration text indexing of the loaded data.
     * Value should be "default" (to index rdfs:label) or a comma-separated list of predicates to index. These can
     * use curies with the prefixes as defined in the application's prefix service.
     * Index will always include rdfs:label.
     */
    public void setTextIndex(String spec) {
        this.indexSpec = spec;
    }
    
    public void setUnionDefault(boolean flag) {
        isUnionDefault = flag;
    }
    
    @Override
    public void startup(App app) {
        super.startup(app);
        dataset = TDBFactory.createDataset( tdbDir.getPath() );
        if (textIndex != null) {
            try {
                Directory dir = FSDirectory.open(textIndex);
                EntityDefinition entDef = new EntityDefinition("uri", "text", RDFS.label.asNode()) ;
                if (indexSpec != null) {
                    for (String spec : indexSpec.split(",")) {
                        String uri = getApp().getPrefixes().expandPrefix(spec.trim());
                        if ( ! uri.equals("default") ) {
                            Node predicate = NodeFactory.createURI(uri);
                            if (!predicate.equals(RDFS.label.asNode())) {
                                entDef.set("text", predicate);
                            }
                        }
                    }
                }
                dataset = TextDatasetFactory.createLucene(dataset, dir, entDef, new StandardAnalyzer(org.apache.jena.query.text.TextIndexLucene.VER)) ;            
            } catch (IOException e) {
                throw new EpiException("Failed to create jena-text lucence index area", e);
            }
        }
    }
    
    @Override
    protected QueryExecution start(String queryString) {
        Query query = QueryFactory.create(queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, dataset) ;
        if (isUnionDefault) {
            qexec.getContext().set(TDB.symUnionDefaultGraph, true) ;
        }
        dataset.begin(ReadWrite.READ);
        return qexec;
    }
    
    @Override
    protected void finish(QueryExecution qexec) {
        qexec.close() ;
        dataset.end();
    }

    @Override
    public void update(UpdateRequest update) {
        dataset.begin(ReadWrite.WRITE);
        UpdateExecutionFactory.create(update, getGraphStore()).execute();        
        dataset.commit();
    }

    @Override
    public boolean isUpdateable() {
        return true;
    }


    @Override
    public DatasetAccessor getAccessor() {
        if (accessor == null) {
            accessor = new TransactionalDatasetAccessor(dataset);
        }
        return accessor;
    }
    
    protected DatasetGraph getGraphStore() {
        if (graphStore == null) {
            graphStore = DatasetGraphFactory.create(dataset.asDatasetGraph());
        }
        return graphStore;
    }

    /**
     * Return an action that will backup the data to the backup directory
     */
    public Action getBackupAction() {
        return new BackupAction();
    }
    
    public class BackupAction extends BaseAction {
        
        @Override
        public String getName() {
            return "backup";
        }

        @Override
        protected JsonObject doRun(JsonObject parameters,
                ProgressMonitorReporter monitor) {
            if (backupDir == null) {
                log.error("No backup directory configured");
                monitor.reportError("No backup directory configured");
                return JsonUtil.emptyObject();
            }
            String filename = "backup-" + TimeStamp.makeTimestamp() + ".nq.gz";
            File backupFile = new File(backupDir, filename);

            monitor.report("Backup in progress: " + filename);
            log.info("Started  backup to " +filename);
            OutputStream out = null ;
            dataset.begin(ReadWrite.READ);
            try {
                out = new FileOutputStream(backupFile) ;
                out = new GZIPOutputStream(out, 8*1024) ;
                out = new BufferedOutputStream(out) ;
                
                RDFDataMgr.write(out, dataset.asDatasetGraph(), Lang.NQUADS) ;
                out.close() ;
                out = null ;

                monitor.report("Backup finished: " + filename);
                log.info("Finished backup to " + filename);
                return JsonUtil.makeJson("backupfile", backupFile.getPath());
                
            } catch (IOException e) {
                log.warn("Problem writing backup to " + filename, e);
                monitor.reportError("Problem writing backup to " + filename + ", " + e);
                
            } catch ( RuntimeException ex ) {
                log.warn("Exception during backup: ", ex);
                monitor.reportError("Exception during backup: " + ex);
                
            } finally {
                dataset.end();
                try { if (out != null) out.close() ; }
                catch (IOException e) { /* ignore */ }
            }
            return JsonUtil.emptyObject();
        }
        
    }
    
}
