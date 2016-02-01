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
        assertEquals("http://example.com/foo.ttl?param=42", x.setExtensionFrom("http://example.com/foo.ttl?p=q").toString());
        
        assertEquals("http://example.com/foo?param=42", u.asLocal("http://localhost/").toString());
        assertEquals("/foo?param=42", u.asLocal("http://example.com/foo").toString());

    }
}
