/******************************************************************
 * File:        TestBasicRender.java
 * Created by:  Dave Reynolds
 * Created on:  27 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.epimorphics.appbase.webapi.testing.TomcatTestBase;
import com.sun.jersey.api.client.ClientResponse;

public class TestBasicRender extends TomcatTestBase {

    @Override
    public String getWebappRoot() {
        return  "src/test/exampleApp";
    }
    
    @Test
    public void testRender() {
        ClientResponse response = getResponse(BASE_URL + "/test?arg=foo", "text/hml");
        assertEquals(200, response.getStatus());
        
        // Should do testing base on HTML structure if can figure the right library for that, in the meantime ...
        List<String> paras = findmatches(response.getEntity(String.class), "<p>([^<]*)</p>");
        assertEquals("Hello there from myapp (parameter = This is a string)", paras.get(0));
        assertEquals("Query param arg = foo", paras.get(1));
        assertEquals("Component1.prop1 = Component 1 name", paras.get(2));
        assertEquals("Library plugin: Hello from lib plugin - myplugin in application myapp", paras.get(3));
    }
    
    static List<String> findmatches(String page, String regex) {
        Matcher m = Pattern.compile(regex).matcher(page);
        List<String> matches = new ArrayList<>();
        while (m.find()) {
            matches.add( m.group(1) );
        }
        return matches;
    }
}
