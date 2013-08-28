/******************************************************************
 * File:        TestSource.java
 * Created by:  Dave Reynolds
 * Created on:  28 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestSource extends BaseSourceTest {

    @Override
    protected String getTestFileDir() {
        return "src/test/data/source-tests/base";
    }

    @Test
    public void testLabels() {
        assertEquals("Pref label", getNode("test:i1").getLabel());
        assertEquals("Alt label",  getNode("test:i2").getLabel());
        assertEquals("rdfs label", getNode("test:i3").getLabel());
        assertEquals("name",       getNode("test:i4").getLabel());
    }
}
