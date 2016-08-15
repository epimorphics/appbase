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

package com.epimorphics.appbase.templates;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.util.EpiException;

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
 *    <li>plugins - an optional list of plugin objects which should be attached to Lib</li>
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
 *   <li>app - the App which owns this rendererr</li>
 *   <li>all request parameter names bound to their values in the request</li>
 *   <li>all App components bound to their names</li>
 *   <li>call-specific bindings which may replace any of the above </li>
 *  </ul>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class VelocityRender extends ComponentBase {
    private static final String CONTEXT = "context";
    public static final String CONFIG_FILENAME = "velocity.properties";
    public static final String MACRO_FILE      = "macros.vm";
    public static final String FILTER_NAME     = "VelocityRenderer";
    public static final String MANUAL_PARAM    = "manualConfig";

    static Logger log = LoggerFactory.getLogger(VelocityRender.class);

    protected VelocityEngine ve;
    protected boolean isProduction;
    protected File templateDir;
    protected String rootURI;
    protected Lib theLib = new Lib();
    FilterRegistration registration;
    protected String loggerName;
    
    public void setProduction(boolean isProduction) {
        this.isProduction = isProduction;
    }

    public void setTemplates(String templates) {
        templateDir = asFile(templates);
        if (!templateDir.isDirectory() || !templateDir.canRead()) {
            throw new EpiException("Can't access velocity template directory: " + templateDir);
        }
    }

    public void setRoot(String rootURI) {
        if ( !rootURI.startsWith("/") ) {
            rootURI = "/" + rootURI;
        }
        if ( rootURI.endsWith("*") ) {
            rootURI = rootURI.substring(0,  rootURI.length()-1);
        }
        if (rootURI.endsWith("/")) {
            rootURI = rootURI.substring(0,  rootURI.length()-1);
        }
        this.rootURI = rootURI;
    }
    
    public void setPlugins(List<LibPlugin> plugins) {
        for (LibPlugin plugin: plugins) {
            setPlugin(plugin);
        }
    }
    
    public void setPlugin(LibPlugin plugin) {
        theLib.addPlugin(plugin.getName(), plugin);
    }
    
    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }
    
    @Override
    public void startup(App app) {
        super.startup(app);
        
        require(rootURI, "root");
        require(templateDir, "templates");
        
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
            if (loggerName != null) {
                ve.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                                        "org.apache.velocity.runtime.log.Log4JLogChute" );
                ve.setProperty("runtime.log.logsystem.log4j.logger", loggerName);                            
            }

            // Override with any user supplied config
            File configFile = new File(templateDir, CONFIG_FILENAME);
            if (configFile.canRead()) {
                ve.init( configFile.getAbsolutePath() );
                log.info("Loaded config: " + configFile);
            }
        } catch (Exception e) {
            throw new EpiException(e);
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
           String root = request.getServletContext().getContextPath();
           VelocityContext vc = buildContext(root, env);
           Enumeration<String> paramNames = request.getParameterNames();
           while (paramNames.hasMoreElements()) {
               String paramname = paramNames.nextElement();
               vc.put( paramname, request.getParameter(paramname) );
           }
           vc.put( "request", request );
           vc.put( "response", response );

           template.merge(vc, out);
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
     */
    public StreamingOutput render(String templateName, String requestURI, ServletContext context, MultivaluedMap<String, String> parameters, Object...args) {
        final Template template = ve.getTemplate(templateName);     // Throws exception if not found
        final VelocityContext vc = buildContext(requestURI, context, parameters, args);
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
  
    /**
     * Variant of render suitable for use from jax-rs implementations.
     * The environment will include the library (lib), 
     * the root context for the container (root), and
     * the set of configured services and the supplied list of bindings.
     */
    public void renderTo(OutputStream output, String templateName, ServletContext context, Map<String, Object> env) throws IOException {
        Template template = ve.getTemplate(templateName);     // Throws exception if not found
        VelocityContext vc = buildContext(context.getContextPath(), env);
        if (vc.get(CONTEXT) == null) {
            vc.put(CONTEXT, context);
        }
        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        template.merge(vc, writer);
        writer.flush();
    }

    protected VelocityContext buildContext( String requestURI, ServletContext context, MultivaluedMap<String, String> parameters, Object...args) {
        VelocityContext vc = buildContext(context.getContextPath(), null);
        vc.put("uri", requestURI);
        vc.put(CONTEXT, context);
        for (String key : parameters.keySet()) {
            List<String> values = parameters.get(key);
            if (values.size() == 1) {
                vc.put(key, escapeQueryParameter( values.get(0) ));
            } else {
                List<String> safeValues = new ArrayList<>( values.size() );
                for (String value : values) {
                    safeValues.add( escapeQueryParameter(value) );
                }
                vc.put(key, safeValues);
            }
            
        }
        for (int i = 0; i < args.length;) {
            String name = args[i++].toString();
            if (i >= args.length) {
                throw new EpiException("Odd number of arguments");
            }
            Object value = args[i++];
            vc.put(name, value);
        }
        return vc;
    }

    protected String escapeQueryParameter( String value ) {
        if (value.contains("<")) {
            return StringEscapeUtils.escapeHtml(value);
        } else {
            return value;
        }
    }
    
    protected VelocityContext buildContext(String root, Map<String, Object> env) {
        VelocityContext vc = new VelocityContext();
        if (root.equals("/")) {
            root = "";
        }
        vc.put( "root", root);
        vc.put( "lib", theLib);
        vc.put( "app", app);
        for (String serviceName : app.listComponentNames()) {
            vc.put(serviceName, app.getComponent(serviceName));
        }
        if (env != null) {
            for (Entry<String, Object> param : env.entrySet()) {
                vc.put(param.getKey(), param.getValue());
            }
        }
        return vc;
    }

}
