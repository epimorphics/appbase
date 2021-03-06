/******************************************************************
 * File:        URLBuilder.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.templates;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.epimorphics.util.EpiException;

/**
 * Utility to help scripts construct URLs. 
 * Assumes the source URL is as raw in the request (i.e. will be URLencoded).
 * It retains the URL encoding internally and when converting to a query
 * but decodes when request for a query parameter value.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class URLBuilder {
    protected String base;
    protected String extension;
    protected String queryString = null;
    protected Map<String, QueryParameter> queryParameters = new HashMap<>();
    
    protected static Pattern EXTPATTERN = Pattern.compile("([^\\?]*)\\.([\\w]*)");
    protected static Pattern BASEPATTERN = Pattern.compile("https?://[^/]*(/.*)");
    
    public URLBuilder(String url) {
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
                String[] parts = query.split("=");
                if (parts.length == 2) {
                    doAddQuery(parts[0], parts[1]);
                } else {
                    doAddQuery(parts[0], null);
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
    
    public String getQueryString() {
        return queryString;
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
    
    public URLBuilder removeQuery(List<String> params) {
        URLBuilder u = clone();
        for (String p : params) {
            u.queryParameters.remove(p);
        }
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
    
    public URLBuilder removeEmptyQueries() {
        URLBuilder u = clone();
        for (String key : queryParameters.keySet()) {
            if (u.queryParameters.get(key).isEmpty()) {
                u.queryParameters.remove(key);
            }
        }
        return u;
    }
    
    public URLBuilder setQuery(String param, String value) {
        URLBuilder u = removeQuery(param);
        u.doAddQuery(param, value);
        return u;
    }
    
    
    public boolean hasQuery(String param) {
        return queryParameters.containsKey(param);
    }
    
    public boolean hasQueryValue(String param) {
        if (hasQuery(param)) {
            return  ! queryParameters.get(param).isEmpty();
        } else {
            return false;
        }
    }
    
    public boolean hasNoQueries() {
        return queryParameters.isEmpty();
    }
    
    public String getFirst(String param) {
        QueryParameter q = queryParameters.get(param);
        if (q != null && ! q.isEmpty()) {
            return decode( q.getValues().get(0) );
        } else {
            return null;
        }
    }
    
    public List<String> getAll(String param) {
        QueryParameter q = queryParameters.get(param);
        if (q != null) {
            List<String> result = new ArrayList<>( q.getValues().size() );
            for (String v : q.getValues() ) {
                result.add( decode(v) );
            }
            return result;
        } else {
            return null;      // Better to return empty list?
        }
    }
    
    public String decode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new EpiException("Java problem - no UTF-8 support");
        }
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
     * then return as a server-relative URL string (i.e. "/context/path.ext?query"),
     * if already server relative then leave alone, otherwise return as an absolute URL with no extension.
     * <p>
     * This may be deprecated in favour of the asSafeLocal implementation if only I can remember
     * why the strange treatment of extensions in external URLs!
     * </p>
     */
    public String asLocal(String rootURL) {
        Matcher m = BASEPATTERN.matcher( toString() );
        if (m.matches()) {
            // Looks like an absolute URL
            if (base.startsWith(rootURL)) {
                // Make server-relative
                return m.group(1);
            } else {
                return removeExtension().toString();
            }
        }
        return toString();
    }
    
    /**
     * If the URL falls within given base (normally the root of an API/UI)
     * then return as a server-relative URL string (i.e. "/context/path.ext?query") otherwise leave along.
     */
    public String asSafeLocal(String rootURL) {
        Matcher m = BASEPATTERN.matcher( toString() );
        if (m.matches()) {
            // Looks like an absolute URL
            if (base.startsWith(rootURL)) {
                // Make server-relative
                return m.group(1);
            }
        }
        return toString();
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
            return values.isEmpty() || (values.size() == 1 && values.get(0).isEmpty() );
        }
        
        @Override
        public String toString() {
            if (values.isEmpty()) {
                return param;
            } else {
                StringBuffer buffer = new StringBuffer();
                boolean started = false;
                for (String value : values) {
                    if (started) {
                        buffer.append("&");
                    } else {
                        started = true;
                    }
                    buffer.append(param);
                    buffer.append("=");
                    buffer.append(value.replace(" ", "+"));
                }
                return buffer.toString();
            }
        }
    }
}
