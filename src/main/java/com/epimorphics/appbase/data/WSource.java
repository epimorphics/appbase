/******************************************************************
 * File:        WSource.java
 * Created by:  Dave Reynolds
 * Created on:  26 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.data.NodeDescription.Coverage;
import com.epimorphics.appbase.data.impl.WResultSetWrapper;
import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.PrefixUtils;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.util.OneToManyMap;

/**
 * A wrapped SPARQL source, designed for easy use from UI scripting.
 * Uses app-wide prefix configuration to expand queries, provides caching
 * of resource descriptions to simplify use of remote sources.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class WSource extends ComponentBase {
    protected static final int DEFAULT_CACHESIZE = 1000;
    
    protected SparqlSource source;
    protected Map<Node, NodeDescription> cache;
        // Cache descriptions rather than nodes so we can mutate a WNode with a new description without thread conflicts
    
    public WSource() {
        setCacheSize(DEFAULT_CACHESIZE);
    }
    
    public void setSource(SparqlSource source) {
        this.source = source;
    }
    
    @SuppressWarnings("unchecked")
    public void setCacheSize(int size) {
        cache = new LRUMap(size);
    }
    
    /**
     * Run a SPARQL query on the data source.
     * 
     * @param query the query to be executed, prefix declarations will be added 
     * from the app-wide prefix settings.
     * @param bindings An array of Objects, which will be taken in pairs to be a string
     * var name and an object to encode as an RDF node.
     */
    public WResultSet select(String query, Object...bindings) {
        String expandedQuery = expandQuery(query);
        if (bindings.length != 0) {
            QuerySolutionMap map = QueryUtil.createBindings(bindings);
            expandedQuery = new ParameterizedSparqlString(expandedQuery, map).toString();
        }
        return new WResultSetWrapper(source.select(expandedQuery), this);
    }
    
    protected String expandQuery(String query) {
        return PrefixUtils.expandQuery(query, getApp().getPrefixes());
    }
    
    // -- Describing and labelling nodes -----------------------------------
    
    /**
     * Return a full description of a set of resources. The descriptions
     * may be returned from a cache or may result in a new query to the 
     * underlying source.
     * This batch call may be cheaper than repeated calls to the nodes themselves.
     */
    public List<WNode> describe(List<WNode> resources) {
        describeList(resources, true);
        return resources;
    }
    
    /**
     * Ensure that each resource in a result set has an associated description.
     * This batch call may be cheaper than repeated calls to the nodes themselves.
     */
    public WResultSet describe(WResultSet results) {
        WResultSet ans = results.copy();
        describe( needDescribing(ans, true) );
        return ans;
    }
    
    /**
     * Return a labelled version a set of resources. The label descriptions
     * may be returned from a cache or may result in a new query to the 
     * underlying source.
     * This batch call may be cheaper than repeated calls to the nodes themselves.
     */
    public List<WNode> label(List<WNode> resources) {
        describeList(resources, false);
        return resources;
    }
    
    protected List<WNode> needDescribing(WResultSet results, boolean needFull) {
        List<WNode> ans = new ArrayList<>();
        for (WQuerySolution row : results) {
            for (String var : row.varNames()) {
                WNode wn = row.get(var);
                if ( ! wn.isDescribed(needFull) ) {
                    ans.add(wn);
                }
            }
        }
        return ans;
    }
    
    protected void describeList(List<WNode> nodes, boolean needFull) {
        List<WNode> batch = new ArrayList<>();
        synchronized (cache) {
            for (WNode node : nodes) {
                if ( ! node.isDescribed(needFull) ) {
                    NodeDescription nd = cache.get(node);
                    if (nd != null && (needFull ? nd.isFullDescription() : nd.hasLabels())) {
                        node.setDescription(nd);
                    } else {
                        batch.add(node);
                    }
                }
            }
        }
        if (!batch.isEmpty()) {
            WNode[] batchArray = new WNode[ batch.size() ];
            batchArray = batch.toArray(batchArray);
            if (needFull) {
                ensureDescribed(batchArray);
            } else {
                ensureLabeled(batchArray);
            }
        }
    }
    
    /**
     * Ensure that each resource in a result set has an associated label
     * This batch call may be cheaper than repeated calls to the nodes themselves.
     */
    public WResultSet label(WResultSet results) {
        WResultSet ans = results.copy();
        label( needDescribing(ans, false) );
        return ans;
    }
    
    protected NodeDescription describe(Node node) {
        NodeDescription description = new NodeDescription(node, source.describeAll(node.getURI()));
        synchronized (cache) {
            cache.put(node, description);
        }
        return description;
    }
    
    protected void ensureLabeled(WNode... nodes) {
        final String labelQuery = "    OPTIONAL {?uri skos:prefLabel ?skos_prefLabel}\n"
                + "    OPTIONAL {?uri skos:altLabel ?skos_altLabel}\n"
                + "    OPTIONAL {?uri rdfs:label ?rdfs_label}\n"
                + "    OPTIONAL {?uri foaf:name ?foaf_name}\n";
        DatasetGraph dsg = constructViews(labelQuery, urisForNodes(nodes));
        synchronized (cache) {
            for (WNode wnode : nodes) {
                Node n = wnode.asNode();
                Graph g = dsg.getGraph(n);
                if (g != null) {
                    NodeDescription nd = new NodeDescription(n, g, Coverage.LABEL);
                    cache.put(n, nd);
                    wnode.setDescription(nd);
                }
            }
        }
    }
    
    protected void ensureDescribed(WNode... nodes) {
        Graph[] graphs = source.describeEach(urisForNodes(nodes));
        synchronized (cache) {
            for (int i = 0; i < nodes.length; i++) {
                WNode wnode = nodes[i];
                Node n = wnode.asNode();
                Graph g = graphs[i];
                NodeDescription nd = new NodeDescription(n, g, Coverage.FULL);
                cache.put(n, nd);
                wnode.setDescription(nd);
            }
        }
    }
                    
    protected String[] urisForNodes(WNode[] batch){
        String[] batchURIs = new String[batch.length];
        for (int i = 0; i < batchURIs.length; i++) {
            batchURIs[i] = batch[i].getURI();
        }
        return batchURIs;
    }
    
    protected String[] urisForNodes(List<WNode> batch){
        String[] batchURIs = new String[batch.size()];
        for (int i = 0; i < batchURIs.length; i++) {
            batchURIs[i] = batch.get(i).getURI();
        }
        return batchURIs;
    }
    
    /**
     * Return a wrapped version of the given node. This will include
     * whatever cached description is already available but will not
     * itself invoke a new query.
     */
    public WNode get(Node node) {
        if (node.isURI()) {
            synchronized (cache) {
                NodeDescription nd = cache.get(node);
                if (nd != null) {
                    return new WNode(this, node, nd);
                }
            }
        }
        return new WNode(this, node);
    }
    
    // -- Text search -------------------------
    

    /**
     * Search for resources which match a text search string. Assumes the
     * associated source has been indexed with Jena text
     */
    public List<WNode> search(String text) {
        return find("SELECT ?x WHERE {?x jtext:query '" + text.replace("'", "\\'") + "' }", "x");
    }

    /**
     * Search for resources which match a text search string. Assumes the
     * associated source has been indexed with Jena text
     */
    public List<WNode> search(String text, int limit) {
        return find( String.format("SELECT ?x WHERE {?x jtext:query ('%s' %d) }", text.replace("'", "\\'"), limit), "x");
    }
    
    // -- Batch queries -----------------------
    
    /**
     * Find a set of notes via a select query
     * @param query the sparql query to run
     * @param var the variable name whose bindings are to be returned
     */
    public List<WNode> find(String query, String var) {
        List<WNode> results = new ArrayList<>();
        for (WQuerySolution row : select(query)) {
            results.add( row.get(var) );
        }
        return results;
    }
    
    
    protected Node asNode(Object prop) {
        if (prop instanceof Node) {
            return (Node)prop;
        } else if (prop instanceof RDFNode) {
            return ((RDFNode)prop).asNode();
        } else if (prop instanceof String) {
            return NodeFactory.createURI( getApp().getPrefixes().expandPrefix((String)prop) );
        } else {
            throw new EpiException("Illegal type used to define property: " + prop);
        }
    }
    
    /**
     * Pseudo construct which uses a simple SELECT query to describe a set of resources and constructs separate
     * graphs for each description. Used to implement labelling of resources.
     * @param queryBody The body of a select query which returns values associated with some ?uri variable. 
     * The body should be just a basic graph pattern, with no SELECT operator. It will be wrapped in a SELECT
     * with an appropriate VALUES statement to inject the actual URIs to be retrieved. Each variable
     * should be of the form prefix_local and will be converted to a curie prefix:local then expanded using
     * the application prefixes to find the property to use to attach the resulting value.   
     * @param uris Set of URIs whose views are to be retrieved
     * @return a dataset with a graph for each described node
     */
    public DatasetGraph constructViews(String queryBody, String... uris) {
        ResultSet rs = source.select( expandQuery( makeViewQuery(queryBody, uris) ) );
        DatasetGraph views = DatasetGraphFactory.createMem();
        Var var = Var.alloc("uri");
        while (rs.hasNext()) {
            Binding binding = rs.nextBinding();
            Node n = binding.get(var);
            Graph view = views.getGraph(n);
            if (view == null) {
                view = new GraphMem();
                views.addGraph(n, view);
            }
            for (Iterator<Var> i = binding.vars(); i.hasNext();) {
                Var v = i.next();
                if (! var.equals(v)) {
                    String vname = v.getVarName();
                    if (vname.contains("_")) {
                        String puri = getApp().getPrefixes().expandPrefix( vname.replace("_", ":") );
                        Node prop = NodeFactory.createURI(puri);
                        view.add( new Triple(n, prop, binding.get(v)) );
                    }
                }
            }
        }
        return views;
    }
    
    // not sure if the follow batch queries are really needed any more ...
    
    /**
     * Query for a view of a set of resources
     * @param queryBody The body of a select query which returns values associated with some ?uri node. 
     * The body should be just a basic graph pattern, with no SELECT operator. It will be wrapped in a SELECT
     * with and appropriate VALUES statement to inject the actual URIs to be retrieved.   
     * @param uris Set of URIs whose views are to be retrieved
     * @return map from the wrapped node corresponding to each uri to a set of varname/value-set maps.
     */
    public  Map<WNode, OneToManyMap<String, WNode>> getViews(String queryBody, String... uris) {
        return getView( makeViewQuery(queryBody, uris), "uri");
    }

    protected String makeViewQuery(String queryBody, String... uris) {
        StringBuffer query = new StringBuffer();
        query.append("SELECT * WHERE {    VALUES ?uri {\n");
        for (String uri : uris) {
            query.append("<" + uri + "> ");
        }
        query.append("    }\n");
        query.append(queryBody);
        query.append("}");
        return query.toString();
    }
    
    /**
     * Aggregates the result of a select into a set of variable -> property/value-set maps.
     * 
     * @param queryString the full SELECT query to be aggregated
     * @param varname the variable to aggregate over
     * @return
     */
    public Map<WNode, OneToManyMap<String, WNode>> getView(String queryString, String varname) {
        ResultSet rs = source.select( expandQuery(queryString) );
        Map<WNode, OneToManyMap<String, WNode>> results = new HashMap<>();
        Var var = Var.alloc(varname);
        while (rs.hasNext()) {
            Binding binding = rs.nextBinding();
            WNode node = get( binding.get(var) );
            OneToManyMap<String, WNode> propValues = results.get( node );
            if (propValues == null) {
                propValues = new OneToManyMap<>();
                results.put(node, propValues);
            }
            for (Iterator<Var> i = binding.vars(); i.hasNext();) {
                Var v = i.next();
                if (! var.equals(v)) {
                    propValues.put(v.getVarName(), get( binding.get(v) ));
                }
            }
        }
        return results;
    }
    
}
