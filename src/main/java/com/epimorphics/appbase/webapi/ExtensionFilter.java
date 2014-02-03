/******************************************************************
 * File:        ExtensionFilter.java
 * Created by:  Dave Reynolds
 * Created on:  3 Feb 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;
import java.io.IOException;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
 
/**
 * Modifies Accept headers and allows URL extensions to improve JAX-RS content negotiation
 * Adds "Vary: accept" header to response
 * 
 * Adapted from: http://www.zienit.nl/blog/2010/01/rest/control-jax-rs-content-negotiation-with-filters
 */
public class ExtensionFilter implements Filter {
 
    private final Map<String,String> extensions = new HashMap<String,String>();
 
    public void init(FilterConfig config) throws ServletException {
        Enumeration<String> exts = config.getInitParameterNames();
        while (exts.hasMoreElements()) {
            String ext = exts.nextElement();
            if (ext != null && !ext.isEmpty()) {
                this.extensions.put("."+ext.toLowerCase(), config.getInitParameter(ext));
            }
        }
    }
 
    public void destroy() {}
 
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        String uri = httpRequest.getRequestURI();
        String ext = this.getExtension(uri);
        String accept = this.extensions.get(ext);
 
        // remove extension and remap the Accept header
        if (!ext.isEmpty()) {
            uri = uri.substring(0, uri.length() - ext.length());
            request = new RequestWrapper(httpRequest, uri, accept);
        }
 
        // add "Vary: accept" to the response headers
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        httpResponse.addHeader("Vary", "accept");
 
        chain.doFilter(request, response);
    }
 
    private String getExtension(String path) {
        int index = path.lastIndexOf('.');
        if (index < 0 || path.lastIndexOf('/') > index) {
            return "";
        }
 
        return path.substring(index);
    }
 
    private static class RequestWrapper extends HttpServletRequestWrapper {
 
        private final String uri;
        private final String accept;
 
        public RequestWrapper(HttpServletRequest request, String uri, String accept) {
            super(request);
 
            this.uri = uri;
            this.accept = accept;
        }
 
        @Override
        public String getRequestURI() {
            return this.uri;
        }
 
        @Override
        public Enumeration<String> getHeaders(String name) {
            if (!"accept".equalsIgnoreCase(name)) {
                return super.getHeaders(name);
            }
 
            Vector<String> values = new Vector<String>(1);
            values.add(this.accept);
            return values.elements();
        }
    }
}
