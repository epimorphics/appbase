/******************************************************************
 * File:        RemoteSparqlSource.java
 * Created by:  Dave Reynolds
 * Created on:  29 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.riot.WebContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.data.SparqlSource;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;

/**
 * Sparql source for querying remote sparql endpoints. Configuration options:
 * <ul>
 *   <li>endpoint - URL for the SPARQL query endpoint</li>
 *   <li>updateEndpoint - optional URL for the SPARQL update endpoint</li>
 *   <li>graphEndpoint - optional URL for the graph store protocol endpoint</li>
 *   <li>contentType - set the type of the data requested for query results, one of "xml", "json", "tsv", "csv"</li>
 * </ul>
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RemoteSparqlSource extends BaseSparqlSource implements SparqlSource {
    static Logger log = LoggerFactory.getLogger(RemoteSparqlSource.class);
    
    static protected Map<String, String> typeMap = new HashMap<String, String>();
    static {
        typeMap.put("xml", WebContent.contentTypeResultsXML);
        typeMap.put("json", WebContent.contentTypeResultsJSON);
        typeMap.put("tsv", WebContent.contentTypeTextTSV);
        typeMap.put("csv", WebContent.contentTypeTextCSV);
    }
            
    protected String endpoint;
    protected String contentType = WebContent.contentTypeResultsXML;
    protected String updateEndpoint;
    protected String graphEndpoint;
    protected DatasetAccessor accessor;
    protected long  readTimeout = -1;
    protected long  connectTimeout = -1;
    protected Long  remoteTimeout = null;
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public void setUpdateEndpoint(String endpoint) {
        this.updateEndpoint = endpoint;
    }
    
    public void setGraphEndpoint(String endpoint) {
        this.graphEndpoint = endpoint;
    }
    
    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * The remote timeout is passed to the client on the assumption it's a Fuseki endpoint
     * configured to access a timeout query parameter.
     * @param remoteTimeout timeout in seconds
     */
    public void setRemoteTimeout(long remoteTimeout) {
        this.remoteTimeout = remoteTimeout;
    }
    
    /**
     * Set the content type to request from the remote endpoint.
     * Legal values are "xml", "json", "tsv", "csv".
     */
    public void setContentType(String type) {
        contentType = typeMap.get(type);
        if (contentType == null){
            log.error("Illegal contentType for remote source (" + type + ")");
        }
    }

    @Override
    protected QueryExecution start(String queryString) {
        QueryEngineHTTP hs = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(endpoint, queryString);
        if (contentType != null) {
            hs.setSelectContentType(contentType);
        }
        hs.setTimeout(readTimeout, connectTimeout);
        if (remoteTimeout != null) {
            hs.addParam("timeout", Long.toString(remoteTimeout));
        }
        return hs;
    }

    @Override
    protected void finish(QueryExecution qexec) {
        qexec.close();
    }

    @Override
    public void update(UpdateRequest update) {
        UpdateExecutionFactory.createRemote(update, updateEndpoint).execute();
    }

    @Override
    public boolean isUpdateable() {
        return updateEndpoint != null;
    }

    @Override
    public DatasetAccessor getAccessor() {
        if (accessor == null) {
            accessor = DatasetAccessorFactory.createHTTP(graphEndpoint);
        }
        return accessor;
    }

    
}
