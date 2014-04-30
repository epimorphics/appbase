/******************************************************************
 * File:        PropertyValue.java
 * Created by:  Dave Reynolds
 * Created on:  26 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Property/value pair for wrapped RDF resources.
 * Sortable by local name of property.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class PropertyValue implements Comparable<PropertyValue> {
    protected WNode prop;
    protected List<WNode> values;
    protected boolean sorted = false;
    
    public PropertyValue(WNode prop) {
        this.prop = prop;
        values = new ArrayList<WNode >();
    }
    
    public PropertyValue(WNode prop, WNode value) {
        this.prop = prop;
        values = new ArrayList<WNode>(1);
        values.add(value);
    }

    public List<WNode> getValues() {
        return values;
    }

    public List<WNode> getSortedValues() {
        Collections.sort(values);
        return values;
    }

    public void addValue(WNode value) {
        this.values.add( value );
    }

    public WNode getProp() {
        return prop;
    }

    @Override
    public int compareTo(PropertyValue o) {
        return prop.asResource().getLocalName().compareTo( o.prop.asResource().getLocalName() );
    }
    
}
