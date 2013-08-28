/******************************************************************
 * File:        PropertyValueSet.java
 * Created by:  Dave Reynolds
 * Created on:  28 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Set of property/value bindings for some root resource
* @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
*/
public class PropertyValueSet {
   protected Map<WNode, PropertyValue> pvalues = new HashMap<WNode, PropertyValue>();
   
   public void add(WNode p, WNode value) {
       PropertyValue pv = pvalues.get(p);
       if (pv == null) {
           pv = new PropertyValue(p, value);
           pvalues.put(p, pv);
       } else {
           pv.addValue(value);
       }
   }
   
   public List<PropertyValue> getValues() {
       return new ArrayList<PropertyValue>( pvalues.values() );
   }
   
   public List<PropertyValue> getOrderedValues() {
       List<PropertyValue> result = getValues();
       Collections.sort(result);
       return result;
   }
}
