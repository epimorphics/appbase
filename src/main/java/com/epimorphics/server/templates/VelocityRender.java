/******************************************************************
 * File:        VelocityRender.java
 * Created by:  Dave Reynolds
 * Created on:  2 Dec 2012
 *
 * (c) Copyright 2012, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.server.templates;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.DatasetWrapper;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.core.Store;
import com.epimorphics.server.general.PrefixService;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.FileManager;

/**
 * Service to provide velocity-based HTML rendering. Can be used
 * from Jersey restlets to format returned results. Can also be
 * installed as a filter to render requests directly.
 * <p>
 * Configuration parameters:
 *  <ul>
 *    <li>templates - root directory where velocity templates are located, if it contains a file velocity.properties then
 *        that will be used to configure velocity, if it contains a file macros.vm then that will be used to define static global macros</li>
 *    <li>root - URL, relative to the webapp, where the velocity filter should be installed (so that a request {root}/foo will
 *    test for a file foo.vm in the templates directory and render that, otherwise will forward the filter down the chain)</li>
 *    <li>production - optional property, if set to true then run in production model with full caching</li>
 *  </ul>
 * </p>
 * <p>
 * The velocity templates are run in a context with the following variables set.
 * </p>
 *  <ul>
 *   <li>request - the servlet request object</li>
 *   <li>response - the servlet response object </li>
 *   <li>root - the root context path for the servlet </li>
 *   <li>lib - a java utility library</li>
 *   <li>dataset - wrapper version of the default store</li>
 *   <li>model - wrapper version of the union model of the default store</li>
 *   <li>all request parameter names bound to their values in the request</li>
 *   <li>all registered services available bound to their names</li>
 *   <li>call-specific bindings which may replace any of the above </li>
 *  </ul>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class VelocityRender extends ServiceBase implements Service {
    public static final String TEMPLATES_PARAM = "templates";
    public static final String ROOT_PARAM      = "root";
    public static final String PRODUCTION_PARAM = "production";
    public static final String CONFIG_FILENAME = "velocity.properties";
    public static final String PREFIXES_FILE   = "prefixes.ttl";
    public static final String MACRO_FILE      = "macros.vm";
    public static final String FILTER_NAME     = "VelocityRenderer";
    public static final String PLUGIN_PARAM     = "plugins";
    public static final String MANUAL_PARAM    = "manualConfig";

    static Logger log = LoggerFactory.getLogger(VelocityRender.class);

    protected VelocityEngine ve;
    protected boolean isProduction;
    protected File templateDir;
    protected String rootURI;
    protected PrefixMapping prefixes = null;
    FilterRegistration registration;

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        super.init(config, context);

        templateDir = new File(getRequiredFileParam(TEMPLATES_PARAM));
        if (!templateDir.isDirectory() || !templateDir.canRead()) {
            throw new EpiException("Can't access velocity template directory: " + templateDir);
        }

        File prefixesFile = new File(templateDir, PREFIXES_FILE);
        if (prefixesFile.canRead()) {
            prefixes = FileManager.get().loadModel(prefixesFile.getAbsolutePath());
            log.info("Loaded prefixes: " + prefixesFile);
        }

        rootURI = getRequiredParam(ROOT_PARAM);
        if ( !rootURI.startsWith("/") ) {
            rootURI = "/" + rootURI;
        }
        if ( rootURI.endsWith("*") ) {
            rootURI = rootURI.substring(0,  rootURI.length()-1);
        }
        if (rootURI.endsWith("/")) {
            rootURI = rootURI.substring(0,  rootURI.length()-1);
        }

        isProduction = "true".equalsIgnoreCase( config.get(PRODUCTION_PARAM) );

        try {
            ve = new VelocityEngine();

            // Default settings
            ve.setProperty( RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templateDir.getAbsolutePath() );
            ve.setProperty( RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, isProduction );
            ve.setProperty( RuntimeConstants.INPUT_ENCODING, "UTF-8" );
            ve.setProperty( RuntimeConstants.OUTPUT_ENCODING, "UTF-8" );
            ve.setProperty( RuntimeConstants.ENCODING_DEFAULT, "UTF-8" );
            if ( new File(templateDir, MACRO_FILE).canRead()) {
                ve.setProperty( RuntimeConstants.VM_LIBRARY, MACRO_FILE);
                ve.setProperty( RuntimeConstants.VM_LIBRARY_AUTORELOAD, !isProduction );
                log.info("Setting macros: " + templateDir + " - " + MACRO_FILE);
            }
            ve.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                    "org.apache.velocity.runtime.log.Log4JLogChute" );

            // Override with any user supplied config
            File configFile = new File(templateDir, CONFIG_FILENAME);
            if (configFile.canRead()) {
                ve.init( configFile.getAbsolutePath() );
                log.info("Loaded config: " + configFile);
            }

            // Install filter
            if (!config.containsKey(MANUAL_PARAM) || config.get(MANUAL_PARAM).equalsIgnoreCase("false")) {
                registration = context.addFilter(FILTER_NAME, new VelocityFilter(this));
                registration.addMappingForUrlPatterns(null, true, rootURI + "/*");
                log.info("Installed velocity render filter at " + rootURI + "/*");
            }
        } catch (Exception e) {
            throw new EpiException(e);
        }
    }

    @Override
    public void postInit() {
        String plugins = config.get(PLUGIN_PARAM);
        if (plugins != null) {
            for (String pluginName : plugins.split("\\|")) {
                LibPlugin plugin = ServiceConfig.get().getServiceAs(pluginName, LibPlugin.class);
                if (plugin != null) {
                    Lib.theLib.addPlugin(pluginName, plugin);
                }
            }
        }
        
        // Can use global prefix definition if there isn't a local one for this renderer
        if (prefixes == null) {
            PrefixService pservice = ServiceConfig.get().getFirst(PrefixService.class);
            if (pservice != null) {
                prefixes = pservice.getPrefixes();
            }
        }
    }

    /**
     * Find velocity template that matches the request path. If one exists render it and return true, otherwise return false.
     */
    public boolean render(HttpServletRequest request, HttpServletResponse response, Map<String, Object> env) {
        String templatename = request.getServletPath();
        if (request.getPathInfo() != null) {
            // If we have the default servlet bound to /* we end up with the path here instead of in the servletPath
            templatename += request.getPathInfo();
        }
        if (templatename.startsWith(rootURI)) {  // Should always be true
            templatename = templatename.substring(rootURI.length());
        }
        if (templatename.startsWith("/")) {
            templatename = templatename.substring(1);
        }
        templatename += ".vm";
        try {
            render(templatename, request, response, env);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw new EpiException(e);
        }
    }

    /**
     * Render the given template,
     */
    public void render(String templateName, HttpServletRequest request, HttpServletResponse response, Map<String, Object> env) throws ResourceNotFoundException, IOException {
       Template template = ve.getTemplate(templateName);     // Throws exception if not found
       response.setContentType("text/html");
       response.setStatus(HttpServletResponse.SC_OK);
       response.setCharacterEncoding("UTF-8");
       PrintWriter out = response.getWriter();
       try {
           template.merge(buildContext(request, response, env), out);
       } catch (Exception e) {
           log.error("Exception executing template: " + templateName, e);
           throw new EpiException(e);
       }
       out.close();
    }

    /**
     * Variant of render suitable for use from jax-rs implementations.
     * The environment will include the library (lib), the request URI (uri),
     * the root context for the container (root), and
     * the set of configured services and the supplied list of bindings.
     *
     * @param templateName  the template to render
     * @param args   an alternative sequence of names and java objects to inject into the environment
     * @return
     */
    public StreamingOutput render(String templateName, String requestURI, ServletContext context, MultivaluedMap<String, String> parameters, Object...args) {
        final Template template = ve.getTemplate(templateName);     // Throws exception if not found
        final VelocityContext vc = new VelocityContext();
        vc.put("uri", requestURI);
        vc.put("context", context);
        String root = context.getContextPath();
        if (root.equals("/")) {
            root = "";
        }
        vc.put( "root", root);
        vc.put( "lib", Lib.theLib);
        for (String serviceName : ServiceConfig.get().getServiceNames()) {
            vc.put(serviceName, ServiceConfig.get().getService(serviceName));
        }
        for (String key : parameters.keySet()) {
            vc.put(key, parameters.getFirst(key));
        }
        for (int i = 0; i < args.length;) {
            String name = args[i++].toString();
            if (i >= args.length) {
                throw new EpiException("Odd number of arguments");
            }
            Object value = args[i++];
            vc.put(name, value);
        }
        return new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
                template.merge(vc, writer);
                writer.flush();
            }
        };
    }

    public void setPrefixes(PrefixMapping pm) {
        prefixes = pm;
    }

    public PrefixMapping getPrefixes() {
        return prefixes;
    }

    protected VelocityContext buildContext(HttpServletRequest request, HttpServletResponse response, Map<String, Object> env) {
        VelocityContext vc = new VelocityContext();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramname = paramNames.nextElement();
            vc.put( paramname, request.getParameter(paramname) );
        }
        vc.put( "request", request );
        vc.put( "response", response );
        String root = request.getServletContext().getContextPath();
        if (root.equals("/")) {
            root = "";
        }
        vc.put( "root", root);
        vc.put( "lib", Lib.theLib);
        for (String serviceName : ServiceConfig.get().getServiceNames()) {
            vc.put(serviceName, ServiceConfig.get().getService(serviceName));
        }
        Store defaultStore = ServiceConfig.get().getFirst(Store.class);
        if (defaultStore != null) {
            DatasetWrapper dsw = new DatasetWrapper(defaultStore.asDataset(), true, prefixes);
            vc.put( "dataset", dsw);
//            vc.put( "model", dsw.getDefaultModelW() );
//            vc.put( "unionmodel", new ModelWrapper( defaultStore.getUnionModel() ) );
        }
        if (env != null) {
            for (Entry<String, Object> param : env.entrySet()) {
                vc.put(param.getKey(), param.getValue());
            }
        }
        return vc;
    }

}
