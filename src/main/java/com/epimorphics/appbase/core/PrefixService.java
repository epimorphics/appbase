/******************************************************************
 * File:        Prefixes.java
 * Created by:  Dave Reynolds
 * Created on:  2 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.FileManager;

/**
 * A service to hold an App-wide set of prefixes used for query expansion
 * and UI scripting.
 * <p>
 * Configuration parameters:
 *  <ul>
 *    <li>prefixFile - RDF file whose prefixes should be loaded</li>
 *  </ul>
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class PrefixService extends ComponentBase {
    static Logger log = LoggerFactory.getLogger(PrefixService.class);

    protected static PrefixMapping globalDefault;
    protected static final String DEFAULT_PREFIXES = "defaultPrefixes.ttl";
    
    static {
        globalDefault = FileManager.get().loadModel(DEFAULT_PREFIXES);
    }
    
    protected PrefixMapping prefixes;
    protected Map<String, Object> jsonldContext;
    
    public PrefixService() {
        prefixes = globalDefault;
    }
    
    public static PrefixMapping getDefault() {
        return globalDefault;
    }
    
    public void setPrefixFile(String file) {
        File pf = asFile(file);
        if (pf.canRead()) {
            prefixes = FileManager.get().loadModel(pf.getAbsolutePath());
            log.info("Loaded prefixes: " + pf);
            prefixes.setNsPrefixes(globalDefault);
        } else {
            log.error("Failed to find prefixes file: " + pf);
        }
    }

    public void setPrefixes(PrefixMapping pm) {
        prefixes = pm;
        jsonldContext = null;
    }

    public PrefixMapping getPrefixes() {
        return prefixes;
    }
    
    /**
     * Return a JSON-LD context declaring all the known prefixes
     */
    public Map<String, Object> asJsonldContext() {
        if (jsonldContext == null) {
            jsonldContext = new HashMap<String, Object>();
            Map<String, String> map = prefixes.getNsPrefixMap();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                jsonldContext.put(entry.getKey(), entry.getValue());
            }
        }
        return jsonldContext;
    }
    
    /**
     * Find a shortname for a resource to use in APIs.
     * This will be a curie for its URI, otherwise
     * its full URI.
     */
    public String getResourceID(Resource resource) {
        if (resource.isURIResource()) {
            return prefixes.shortForm(resource.getURI());
        } else {
            return resource.getId().getLabelString();
        }
    }

}
