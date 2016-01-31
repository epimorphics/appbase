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

public class TestLib {

    @Test
    public void testBasics() {
        Lib lib = new Lib();
        String testURL = "http://localhost/foo?param=42";
        assertEquals("http://localhost/foo", lib.removeQueryParam(testURL, "param"));
        assertEquals("http://localhost/foo", lib.removeQueryParam("http://localhost/foo", "param"));
        assertEquals("http://localhost/foo?param=42&bar=1", lib.addQueryParam(testURL, "bar", 1));
        assertEquals(testURL, lib.addQueryParam("http://localhost/foo", "param", 42));
    }
}
