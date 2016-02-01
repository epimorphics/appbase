/******************************************************************
 * File:        URLBuilder.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to help scripts construct URLs. 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class URLBuilder {
    protected String base;
    protected String extension;
    protected Map<String, QueryParameter> queryParameters = new HashMap<>();
    
    protected static Pattern EXTPATTERN = Pattern.compile("([^\\?]*)\\.([\\w]*)");
    protected static Pattern BASEPATTERN = Pattern.compile("https?://[^/]*(/.*)");
    
    public URLBuilder(String url) {
        String queryString = null;
        int split = url.indexOf('?');
        if (split > 0) {
            base = url.substring(0, split);
            queryString = url.substring(split+1);
        } else {
            base = url;
        }
        Matcher matcher = EXTPATTERN.matcher(base);
        if (matcher.matches()) {
            base = matcher.group(1);
            extension = matcher.group(2);
        }
        if (queryString != null) {
            for (String query : queryString.split("&")) {
                if (query.contains("=")) {
                    String[] parts = query.split("=");
                    doAddQuery(parts[0], parts[1]);
                } else {
                    doAddQuery(query, null);
                }
            }
        }
    }
    
    public URLBuilder(String base, String extension, Map<String, QueryParameter> queryParameters) {
        this.base = base;
        this.extension = extension;
        this.queryParameters = new HashMap<>();
        for (Map.Entry<String, QueryParameter> e : queryParameters.entrySet()) {
            this.queryParameters.put(e.getKey(), e.getValue().clone());
        }
    }
    
    public URLBuilder clone() {
        return new URLBuilder(base, extension, queryParameters);
    }

    protected void doAddQuery(String param, String value) {
        QueryParameter qp = queryParameters.get(param);
        if (qp == null){
            qp = new QueryParameter(param);
            queryParameters.put(param, qp);
        }
        if (value != null) {
            qp.addValue(value);
        }
    }
    
    public URLBuilder addQuery(String param, String value) {
        URLBuilder u = clone();
        u.doAddQuery(param, value);
        return u;
    }
    
    public URLBuilder removeQuery(String param) {
        URLBuilder u = clone();
        u.queryParameters.remove(param);
        return u;
    }
    
    public URLBuilder removeQuery(String param, String value) {
        URLBuilder u = clone();
        QueryParameter qp = u.queryParameters.get(param);
        if (qp != null) {
            qp.removeValue(value);
            if (qp.isEmpty()) {
                u.queryParameters.remove(param);
            }
        }
        return u;
    }
    
    public String getExtension() {
        return extension;
    }
    
    public URLBuilder setExtension(String extension) {
        return new URLBuilder(base, extension, queryParameters);
    }
    
    public URLBuilder removeExtension() {
        return new URLBuilder(base, null, queryParameters);
    }
    
    public URLBuilder setExtensionFrom(Object otherURL){
        URLBuilder other = null;
        if (otherURL instanceof URLBuilder) {
            other = (URLBuilder) otherURL;
        } else if (otherURL != null) {
            other = new URLBuilder(otherURL.toString());
        }
        if (other != null) {
            return setExtension(other.getExtension());
        }
        return this;
    }
    
    public URLBuilder addSegment(String segment) {
        return new URLBuilder(base + segment, extension, queryParameters);
    }
    
    public URLBuilder removeQueries() {
        return new URLBuilder(base, extension, new HashMap<>());
    }
    
    /**
     * If the URL falls within given base (normally the root of an API/UI)
     * then return as a server-relative URL string (i.e. "/context/path.ext?query")
     * otherwise return as an absolute URL with no extension.
     */
    public String asLocal(String rootURL) {
        if (base.startsWith(rootURL)) {
            // Make server-relative
            Matcher m = BASEPATTERN.matcher( toString() );
            if (m.matches()) {
                return m.group(1);
            }
        }
        return removeExtension().toString();
    }
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(base);
        if (extension != null) {
            buff.append(".");
            buff.append(extension);
        }
        if (!queryParameters.isEmpty()) {
            buff.append("?");
            boolean started = false;
            // Order for ease of testing - TODO should we preserve original order
            List<String> params = new ArrayList<>( queryParameters.keySet());
            Collections.sort(params);
            for (String param : params) {
                QueryParameter qp = queryParameters.get(param);
                if (started) {
                    buff.append("&");
                } else {
                    started = true;
                }
                buff.append( qp.toString() );
            }
        }
        return buff.toString();
    }
    
    public static class QueryParameter {
        protected String param;
        protected ArrayList<String> values = new ArrayList<String>();
        
        public QueryParameter(String param) {
            this.param = param;
        }
        
        @SuppressWarnings("unchecked")
        public QueryParameter(String param, ArrayList<String> values) {
            this.param = param;
            this.values = (ArrayList<String>) values.clone();
        }
        
        public QueryParameter(String param, String value) {
            this.param = param;
            values.add(value);
        }
        
        public String getParam() {
            return param;
        }
        
        public List<String> getValues() {
            return values;
        }
        
        public void setValue(String value) {
            values.clear();
            addValue(value);
        }
        
        public void addValue(String value) {
            if (value != null) {
                values.add(value);
            }
        }
        
        public void removeValue(String value) {
            values.remove(value);
        }
        
        public QueryParameter clone() {
            return new QueryParameter(param, values);
        }
        
        public boolean isEmpty() {
            return values.isEmpty();
        }
        
        @Override
        public String toString() {
            if (values.isEmpty()) {
                return param;
            } else {
                StringBuffer buffer = new StringBuffer();
                for (String value : values) {
                    buffer.append(param);
                    buffer.append("=");
                    buffer.append(value);
                }
                return buffer.toString();
            }
        }
    }
}
