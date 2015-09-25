/******************************************************************
 * File:        Lib.java
 * Created by:  Dave Reynolds
 * Created on:  20 Apr 2011
 *
 * (c) Copyright 2011, Epimorphics Limited
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
 * $Id:  $
 *****************************************************************/

package com.epimorphics.appbase.templates;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.jena.atlas.json.JsonValue;

import com.epimorphics.appbase.data.WNode;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.rdfutil.RDFNodeWrapper;
import com.epimorphics.util.NameUtils;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.XSDDateType;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.sparql.util.NodeFactoryExtra;

/**
 * Collection of utility functions to be made available
 * within the scripting environment (e.g. velocity)
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 * @version $Revision: $
 */
public class Lib {

    /** Singleton instance */
    public static Lib theLib = new Lib();

    private Map<String, LibPlugin> plugins = new HashMap<String, LibPlugin>();

    public LibPlugin get(String plugin) {
        return plugins.get(plugin);
    }

    public void addPlugin(String pluginname, LibPlugin plugin) {
        plugins.put(pluginname, plugin);
    }

    /**
     * Encode a string so it can be used in a query parameter or path segment safely.
     * May be over conserv
     * ative on encode. Uses %-encoding.
     */
    public String pathEncode(String orig) {
        return NameUtils.encodeSafeName(orig);
    }

    /**
     * Decode query parameter or path segment that was %-encoded
     */
    public String pathDecode(String enc) {
        return NameUtils.decodeSafeName(enc);
    }
    
    /** 
     * Map a string to something safe to use in anchors etc
     */
    public String safeName(String orig) {
        return NameUtils.safeVarName(orig);
    }

    /**
     * Convert a string arg to an integer, returning the default
     * value if the parse failes
     */
    public int safeInt(String lex, int def) {
        try {
            return Integer.parseInt(lex);
        } catch (Exception e) {
            return def;
        }

    }

    /**
     * Match a string to a regex and return a vector of the matching groups
     */
    public String[] regex(Object data, String regex) {
        Matcher m = Pattern.compile(regex).matcher(data.toString());
        if (m.matches()) {
            String[] result = new String[ m.groupCount()];
            for (int i = 0; i < m.groupCount(); i++) {
                result[i] = m.group(i+1);
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * HTML escape text
     */
    public String escapeHtml(String html) {
        return StringEscapeUtils.escapeHtml(html);
    }

    /**
     * HTML escape text and limit to N characters
     */
    public String escapeHtml(String html, int limit) {
        if (html.length() > limit) {
            return escapeHtml(html.substring(0, limit-3)) + "...";
        } else {
            return escapeHtml(html);
        }
    }
    
    /**
     * Limit a string to at most N characters
     */
    public String strLimit(String s, int N) {
        return s.length() > N ? s.substring(0, N) : s;
    }

    /**
     * Transform a string with a regex replacement
     */
    public String replaceAll(Object data, String regex, String replacement) {
        return data.toString().replaceAll(regex, replacement);
    }

    /**
     * Return a sorted list of the keys in a map
     */
    public List<String> sortedKeys(Map<String, ?> map) {
        List<String> keys = new ArrayList<String>( map.keySet().size() );
        keys.addAll( map.keySet() );
        Collections.sort(keys);
        return keys;
    }

    /**
     * Serialize a (URI or literal) RDFNode to a string which can be later parsed
     */
    public static String serialize(RDFNode node) {
        return FmtUtils.stringForNode(node.asNode());
    }

    /**
     * Decode a serlialized RDFNode, not associated with any useful model
     */
    public static RDFNode deserialize(String ser) {
        Node n = NodeFactoryExtra.parseNode(ser);
//        Node n = NodeFactory.parseNode(ser);
        if (n.isLiteral()) {
            return new LiteralImpl(n, dummy);
        } else if (n.isURI()) {
            return ResourceFactory.createResource(n.getURI());
        } else {
            return dummy.createResource();
        }
    }

    protected static ModelCom dummy = (ModelCom) ModelFactory.createDefaultModel();

    /**
     * Paging helper. Generate request for a numbered page
     */
    public String pageURL(HttpServletRequest request, int page) {
        String url = request.getRequestURI();
        Enumeration<String> names = request.getParameterNames();
        boolean started = false;
        while (names.hasMoreElements()) {
            if (started) {
                url += "&";
            } else {
                url += "?";
                started = true;
            }
            String param = names.nextElement();
            if ( ! param.equals(PAGE_PARAM)) {
                url += param + "=" + pathEncode( request.getParameter(param) );
            }
        }
        if (started) {
            url += "&";
        } else {
            url += "?";
        }
        url += PAGE_PARAM + "=" + page;
        return url;
    }
    static final String PAGE_PARAM = "page";

    /**
     * Return the current time as a unix Long time stamp
     */
    public Long now() {
        return System.currentTimeMillis();
    }
    
    /**
     * Return the current time as a Calendar object
     */
    public Calendar calendar() {
        return Calendar.getInstance();
    }
    
    /**
     * Test if a node is a date-time literal
     */
    public boolean isDatetime(Object node) {
        return asDateTime(node) != null;
    }

    /**
     * Pretty print a datetime literal.
     * Returns null if it is not a date time
     */
    public String printDatetime(String format, Object node) {
        Date d = asDateTime(node);
        if (d != null) {
            return new SimpleDateFormat(format).format(d);
        }
        return null;
    }

    /**
     * Pretty print a datetime literal.
     * Returns null if it is not a date time
     */
    public String printDatetime(Object node) {
        return printDatetime("d MMM yyyy HH:mm:ss.SSS", node);
    }

    public Date asDateTime(Object node) {
        RDFNode n = null;
        if (node instanceof Date) {
            return (Date)node;
        } else if (node instanceof Calendar) {
            return ((Calendar)node).getTime();
        } else if (node instanceof Long) {
            return new Date( (Long)node );
        }
        if (node instanceof RDFNodeWrapper) {
            n = ((RDFNodeWrapper)node).asRDFNode();
        } else if (node instanceof RDFNode) {
            n = (RDFNode) node;
        } else if (node instanceof WNode && ((WNode)node).isLiteral()) {
            n = ((WNode) node).asLiteral();
        } else {
            return null;
        }
        if (n.isLiteral()) {
            Literal l = n.asLiteral();
            RDFDatatype dt = l.getDatatype();
            if (XSDDateType.XSDdateTime.equals(dt) || XSDDateType.XSDdate.equals(dt)) {
                Calendar c = ((XSDDateTime)l.getValue()).asCalendar();
                return c.getTime();
            }
        }
        return null;
    }

    /**
     * Convert a riot Json value to plain Map/List style objects
     */
    public Object fromJson(JsonValue jv) {
        return JsonUtil.fromJson(jv);
    }
    
    /**
     * Convert plain java Map/List structure to a riot Json object
     */
    public JsonValue toJson(Object o) {
        return JsonUtil.asJson(o);
    }

    
    // Simple facet support - multi-valued query parameter holds encoded URIs that have been selected
    
    public boolean facetContains(Object facet, String value) {
        return asFacet(facet).contains(value);
    }
    
    public List<String> addToFacet(Object facet, String value) {
        List<String> result = new ArrayList<>(asFacet(facet));
        result.add(value);
        return result;
    }
    
    public List<String> removeFromFacet(Object facet, String value) {
        List<String> result = new ArrayList<>( asFacet(facet) );
        result.remove(value);
        return result;
    }
    
    public String facetAsQuery(String queryName, Object facet) {
        boolean started = false;
        StringBuffer query = new StringBuffer();
        for (String value : asFacet(facet)) {
            query.append( started ? "&" : "?");
            started = true;
            query.append(queryName);
            query.append("=");
            query.append( pathEncode(value) );
        }
        return query.toString();
    }
    
    @SuppressWarnings("unchecked")
    private List<String> asFacet(Object facet) {
        if (facet instanceof String) {
            return Collections.singletonList((String)facet);
        } else if (facet instanceof List<?>) {
            return (List<String>)facet;
        } else {
            return Collections.emptyList();
            
        }
    }
}

