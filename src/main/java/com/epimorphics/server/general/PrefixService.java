/******************************************************************
 * File:        Prefixes.java
 * Created by:  Dave Reynolds
 * Created on:  2 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.server.general;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.ServiceConfig;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.FileManager;

/**
 * A service to make available a predefined set of prefixes.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class PrefixService extends ServiceBase implements Service {
    static Logger log = LoggerFactory.getLogger(PrefixService.class);
    
    protected final static String PREFIXES_FILE_PARAM = "prefixes";

    protected static PrefixService globalDefault;
    
    protected PrefixMapping prefixes;
    protected Map<String, Object> jsonldContext;
    
    @Override
    public void init(Map<String, String> config, ServletContext context) {
        super.init(config, context);
        
        File prefixesFile = new File( getRequiredFileParam(PREFIXES_FILE_PARAM) );
        if (prefixesFile.canRead()) {
            prefixes = FileManager.get().loadModel(prefixesFile.getAbsolutePath());
            log.info("Loaded prefixes: " + prefixesFile);
        } else {
            log.error("Failed to find prefixes file: " + prefixesFile);
        }
    }

    public void setPrefixes(PrefixMapping pm) {
        prefixes = pm;
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
    
    /**
     * Return the first defined Prefix service we can find, otherwise
     * return a default minimal service.
     */
    public static PrefixService get() {
        if (globalDefault == null) {
            globalDefault = ServiceConfig.get().getFirst(PrefixService.class);
            if (globalDefault == null) {
                globalDefault = new PrefixService();
                globalDefault.setPrefixes( ModelFactory.getDefaultModelPrefixes() );
            }
        }
        return globalDefault;
    }
}
