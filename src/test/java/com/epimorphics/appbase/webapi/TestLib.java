/******************************************************************
 * File:        TestLib.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import org.junit.Test;
import static org.junit.Assert.*;

import com.epimorphics.appbase.templates.Lib;
import com.epimorphics.appbase.templates.URLBuilder;

public class TestLib {

//    @Test
//    public void testBasics() {
//        Lib lib = new Lib();
//        String testURL = "http://localhost/foo?param=42";
//        assertEquals("http://localhost/foo", lib.removeQueryParam(testURL, "param"));
//        assertEquals("http://localhost/foo", lib.removeQueryParam("http://localhost/foo", "param"));
//        assertEquals("http://localhost/foo?param=42&bar=1", lib.addQueryParam(testURL, "bar", 1));
//        assertEquals(testURL, lib.addQueryParam("http://localhost/foo", "param", 42));
//    }
    
    @Test
    public void testURL() {
        Lib lib = new Lib();

        String testURL = "http://example.com/foo?param=42";
        URLBuilder u = lib.asURL(testURL);
        assertEquals(testURL, u.toString());
        assertNull( u.getExtension() );
        
        assertEquals("http://example.com/foo", u.removeQuery("param").toString());
        assertEquals("http://example.com/foo", u.removeQuery("param", "42").toString());
        assertEquals("http://example.com/foo?param=42", u.removeQuery("param", "foo").toString());
        
        assertEquals("http://example.com/foo?param=42&q=foo", u.addQuery("q", "foo").toString());
        
        assertEquals("http://example.com/foo.html?param=42", u.setExtension("html").toString());
        
        URLBuilder x = lib.asURL( "http://example.com/foo.xml?param=42");
        assertEquals("xml", x.getExtension());
        assertEquals("http://example.com/foo.html?param=42", x.setExtension("html").toString());
        assertEquals("http://example.com/foo/bar.xml?param=42", x.addSegment("/bar").toString());
        assertEquals("http://example.com/foo.ttl?param=42", x.setExtensionFrom("http://example.com/foo.ttl?p=q").toString());
        
        assertEquals("http://example.com/foo?param=42", x.asLocal("http://localhost/"));
        assertEquals("/foo.xml?param=42", x.asLocal("http://example.com/foo"));
        
        u = lib.asURL("http://localhost:8080/public-register/waste-carriers-brokers/id?easting=&northing=&name-search=jones&number-search=&address-search=&__postcode=&dist=1");
        assertEquals("jones", u.getFirst("name-search") );
        assertTrue( u.hasQuery("easting") );
        assertFalse( u.hasQueryValue("easting") );
        u = u.removeEmptyQueries();
        assertFalse( u.hasQuery("easting") );
        assertEquals("http://localhost:8080/public-register/waste-carriers-brokers/id?dist=1&name-search=jones", u.toString() );
        assertFalse( u.hasNoQueries() );
        u = u.removeQuery("dist");
        u = u.removeQuery("name-search");
        assertEquals("http://localhost:8080/public-register/waste-carriers-brokers/id", u.toString() );
        assertTrue( u.hasNoQueries() );

    }
}
