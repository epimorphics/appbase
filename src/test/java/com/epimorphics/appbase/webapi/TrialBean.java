/******************************************************************
 * File:        TrialBean.java
 * Created by:  Dave Reynolds
 * Created on:  22 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi;

public class TrialBean {
    protected String prop1;
    protected long proplong;
    protected Object ref;
    
    public TrialBean() {}
    
    public String getProp1() {
        return prop1;
    }
    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }
    public long getProplong() {
        return proplong;
    }
    public void setProplong(long proplong) {
        this.proplong = proplong;
    }
    
    public Object getRef() {
        return ref;
    }
    public void setRef(Object ref) {
        this.ref = ref;
    }
    @Override
    public String toString() {
        return String.format("TrialBean[prop1=%s, proplong=%d, ref=%s]", prop1, proplong, ref == null ? "null" : ref.toString());
    }
}