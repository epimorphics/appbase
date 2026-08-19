package com.epimorphics.appbase.data;

import com.epimorphics.vocabs.SKOS;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourceViewTest {
    private ResourceView view(String uri) {
        Model m = createDefaultModel();
        m.setNsPrefixes(PrefixMapping.Standard);
        m.setNsPrefix("dct", DCTerms.NS);
        m.setNsPrefix("skos", SKOS.NS);
        return new ResourceView(m.createResource(uri));
    }

    @Test
    public void equals_sameUri_returnsTrue() {
        ResourceView rv1 = view("http://example.org/test1");
        ResourceView rv2 = view("http://example.org/test1");
        assertEquals(rv1, rv2);
        assertEquals(rv2, rv1);
    }

    @Test
    public void equals_differentUri_returnsTrue() {
        ResourceView rv1 = view("http://example.org/test1");
        ResourceView rv2 = view("http://example.org/test2");
        assertNotEquals(rv1, rv2);
        assertNotEquals(rv2, rv1);
    }

    @Test
    public void getLabel_withoutLabel_returnsLocalName() {
        ResourceView rv = view("http://example.org/test1");
        assertEquals("test1", rv.getLabel());
    }

    @Test
    public void getLabel_withPrefLabel_returnsPrefLabel() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(SKOS.prefLabel, "test resource");
        assertEquals("test resource", rv.getLabel());
    }

    @Test
    public void getLabel_withAltLabel_returnsAltLabel() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(SKOS.altLabel, "test resource");
        assertEquals("test resource", rv.getLabel());
    }

    @Test
    public void getLabel_withLabel_returnsLabel() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDFS.label, "test resource");
        assertEquals("test resource", rv.getLabel());
    }

    @Test
    public void getLabel_withTitle_returnsTitle() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(DCTerms.title, "test resource");
        assertEquals("test resource", rv.getLabel());
    }

    @Test
    public void getLabel_withName_returnsName() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(FOAF.name, "test resource");
        assertEquals("test resource", rv.getLabel());
    }

    @Test
    public void getLabel_withNick_returnsNick() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(FOAF.nick, "test resource");
        assertEquals("test resource", rv.getLabel());
    }

    @Test
    public void getLabelLang_withoutLabel_returnsLocalName() {
        ResourceView rv = view("http://example.org/test1");
        assertEquals("test1", rv.getLabel("en"));
    }

    @Test
    public void getLabelLang_withPrefLabel_returnsPrefLabel() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(SKOS.prefLabel, createLangLiteral("test resource", "en"));
        assertEquals("test resource", rv.getLabel("en"));
    }

    @Test
    public void getLabelLang_withAltLabel_returnsAltLabel() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(SKOS.altLabel, createLangLiteral("test resource", "en"));
        assertEquals("test resource", rv.getLabel("en"));
    }

    @Test
    public void getLabelLang_withLabel_returnsLabel() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDFS.label, createLangLiteral("test resource", "en"));
        assertEquals("test resource", rv.getLabel("en"));
    }

    @Test
    public void getLabelLang_withTitle_returnsTitle() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(DCTerms.title, createLangLiteral("test resource", "en"));
        assertEquals("test resource", rv.getLabel("en"));
    }

    @Test
    public void getLabelLang_withName_returnsName() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(FOAF.name, createLangLiteral("test resource", "en"));
        assertEquals("test resource", rv.getLabel("en"));
    }

    @Test
    public void getLabelLang_withNick_returnsNick() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(FOAF.nick, createLangLiteral("test resource", "en"));
        assertEquals("test resource", rv.getLabel("en"));
    }

    @Test
    public void getDescription_withoutDescription_returnsEmptyString() {
        ResourceView rv = view("http://example.org/test1");
        assertEquals("", rv.getDescription());
    }

    @Test
    public void getDescription_withDescription_returnsDescription() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(DCTerms.description, "this is a test");
        assertEquals("this is a test", rv.getDescription());
    }

    @Test
    public void getDescription_withDefinition_returnsDefinition() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(SKOS.definition, "this is a test");
        assertEquals("this is a test", rv.getDescription());
    }

    @Test
    public void getDescription_withComment_returnsComment() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDFS.comment, "this is a test");
        assertEquals("this is a test", rv.getDescription());
    }

    @Test
    public void select_returnsQueryResults() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDFS.label, "test");
        rv.root.addProperty(RDFS.label, "appbase");
        ResultSet rs = rv.select("select ?x where { ?res rdfs:label ?x } order by ?x");
        try {
            assertEquals("appbase", rs.next().get("x").asLiteral().getString());
            assertEquals("test", rs.next().get("x").asLiteral().getString());
        } finally {
            rs.close();
        }
    }

    @Test
    public void selectOne_returnsQueryResult() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDFS.label, "test");
        rv.root.addProperty(RDFS.label, "appbase");
        QuerySolution qs = rv.selectOne("select ?x where { ?res rdfs:label ?x } order by ?x");
        assertEquals("appbase", qs.get("x").asLiteral().getString());
    }

    @Test
    public void getSelectedResource_returnsQueryResultResource() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDF.type, DCAT.Catalog);
        Resource res = rv.getSelectedResource("select ?t where { ?res rdf:type ?t }");
        assertEquals(DCAT.Catalog, res);
    }

    @Test
    public void getSelectedResources_returnsQueryResultResource() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDF.type, DCAT.Catalog);
        rv.root.addProperty(RDF.type, DCAT.DataService);
        List<Resource> rs = rv.getSelectedResources("select ?t where { ?res rdf:type ?t } order by ?t");
        assertEquals(DCAT.Catalog, rs.get(0));
        assertEquals(DCAT.DataService, rs.get(1));
    }

    @Test
    public void getConnectedResources() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDF.type, DCAT.Catalog);
        rv.root.addProperty(RDFS.comment, "this is a test");
        rv.root.addProperty(DCTerms.creator, createResource("http://example/org/author"));
        List<Resource> rs = rv.getConnectedResources("rdf:type|dct:creator");
        assertEquals(DCAT.Catalog, rs.get(0));
        assertEquals(createResource("http://example/org/author"), rs.get(1));
    }

    @Test
    public void getConnectedLiterals() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(RDF.type, DCAT.Catalog);
        rv.root.addProperty(RDFS.comment, "this is a test");
        rv.root.addProperty(SKOS.prefLabel, "test resource");
        rv.root.addProperty(DCTerms.creator, createResource("http://example/org/author"));
        List<Literal> ls = rv.getConnectedLiterals("rdfs:comment|skos:prefLabel");
        assertEquals("this is a test", ls.get(0).getString());
        assertEquals("test resource", ls.get(1).getString());
    }

    @Test
    public void getShortId_withoutNotation_returnsLocalName() {
        ResourceView rv = view("http://example.org/test1");
        assertEquals("test1", rv.getShortID());
    }

    @Test
    public void getShortId_withNotation_returnsNotation() {
        ResourceView rv = view("http://example.org/test1");
        rv.root.addProperty(SKOS.notation, "xt1");
        assertEquals("xt1", rv.getShortID());
    }

    @Test
    public void getURI() {
        ResourceView rv = view("http://example.org/test1");
        assertEquals("http://example.org/test1", rv.getURI());
    }

    @Test
    public void getModel() {
        Model m = createDefaultModel();
        ResourceView rv = new ResourceView(m.createResource("http://example.org/test1"));
        assertEquals(m, rv.getModel());
    }

    @Test
    public void getInvResourceValue_withoutInverse_returnsNull() {
        Model m = createDefaultModel();
        Resource r = m.createResource("http://example.org/test1");
        ResourceView rv = new ResourceView(r);
        Resource inv = m.createResource("http://example.org/test2");
        r.addProperty(OWL.sameAs, inv);
        assertEquals(null, rv.getInvResourceValue(OWL.sameAs));
    }

    @Test
    public void getInvResourceValue_withInverse_returnsNull() {
        Model m = createDefaultModel();
        Resource r = m.createResource("http://example.org/test1");
        ResourceView rv = new ResourceView(r);
        Resource inv = m.createResource("http://example.org/test2");
        inv.addProperty(OWL.sameAs, r);
        assertEquals(inv, rv.getInvResourceValue(OWL.sameAs));
    }
}