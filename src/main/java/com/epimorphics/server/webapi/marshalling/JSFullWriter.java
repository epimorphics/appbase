/******************************************************************
 * File:        JsonWriter.java
 * Created by:  Dave Reynolds
 * Created on:  2 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.server.webapi.marshalling;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.json.io.JSWriter;
import org.apache.jena.atlas.lib.Ref;

/**
 * Variant on ARQ streaming JSON writer that supports full JSON numbers.
 * Can't subclass JSWriter it because the underlying writer is private.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class JSFullWriter {
    
    private IndentedWriter out = IndentedWriter.stdout ;
    
    public JSFullWriter() { this(IndentedWriter.stdout) ; }
    public JSFullWriter(OutputStream ps) { this(new IndentedWriter(ps)) ; }
    public JSFullWriter(IndentedWriter ps) { out = ps ; }
    
    public void startOutput() {}
    public void finishOutput() { out.print("\n"); out.flush(); } 
    
    // These apply in nested and flat modes (the difference is controlled by the IndentedWriter
    
    private static String ArrayStart        = "[ " ;
    private static String ArrayFinish       = " ]" ;
    private static String ArraySep          = ", " ; 

    private static String ObjectStart       = "{ " ;
    private static String ObjectFinish      = "}" ;
    private static String ObjectSep         = " ," ;
    private static String ObjectPairSep     = " : " ;
    
    // Remember whether we are in the first element of a compound (object or array). 
    Deque<Ref<Boolean>> stack = new ArrayDeque<Ref<Boolean>>() ;
    
    public void startObject()
    {
        startCompound() ;
        out.print(ObjectStart) ;
        out.incIndent() ;
    }
    
    public void finishObject()
    {
        out.decIndent() ;
        if ( isFirst() )
            out.print(ObjectFinish) ;
        else
        {
            out.ensureStartOfLine() ;
            out.println(ObjectFinish) ;
        }
        finishCompound() ;
    }
    
    public void key(String key)
    {
        if ( isFirst() )
        {
            out.println();
            setNotFirst() ;
        }
        else
            out.println(ObjectSep) ;
        value(key) ;
        out.print(ObjectPairSep) ;
        // Ready to start the pair value.
    }
    
    // "Pair" is the name used in the JSON spec. 
    public void pair(String key, String value)
    {
        key(key) ;
        value(value) ;
    }
    
     
    public void pair(String key, boolean val)
    {
        key(key) ;
        value(val) ;
    }

    public void pair(String key, Number val)
    {
        key(key) ;
        value(val) ;
    }

    public void pair(String key, long val)
    {
        key(key) ;
        value(val) ;
    }

    public void startArray()
    {
        startCompound() ;
        out.print(ArrayStart) ;
        // Messy with objects out.incIndent() ;
    }
     
    public void finishArray()
    {
//        out.decIndent() ;
        out.print(ArrayFinish) ;       // Leave on same line.
        finishCompound() ;
    }

    public void arrayElement(String str)
    {
        arrayElementProcess() ;
        value(str) ;
    }

    public void arrayElementProcess()
    {
        if ( isFirst() )
            setNotFirst() ;
        else
            out.print(ArraySep) ;
    }
    
    public void arrayElement(boolean b)
    {
        arrayElementProcess() ;
        value(b) ;
    }

    public void arrayElement(long integer)
    {
        arrayElementProcess() ;
        value(integer) ;
    }
    
    /**
     * Useful if you are manually creating arrays and so need to print array separators yourself
     */
    public void arraySep()
    {
        out.print(ArraySep);
    }
    
    public static String outputQuotedString(String string)
    {
        IndentedLineBuffer b = new IndentedLineBuffer() ;
        JSWriter.outputQuotedString(b, string) ;
        return b.asString() ;
    }
    
    public void print(String x) {
        out.print(x);
    }
    
    private void startCompound()    { stack.push(new Ref<Boolean>(true)) ; }
    private void finishCompound()   { stack.pop(); }
    private boolean isFirst()       { return stack.peek().getValue() ; }
    private void setNotFirst()      { stack.peek().setValue(false) ; }
    
    // Can only write a value in some context.
    private void value(String x) { out.print(outputQuotedString(x)); }
    
    private void value(boolean b) { out.print(Boolean.toString(b)) ; }
    
    private void value(long integer) { out.print(Long.toString(integer)) ; }
    
    private void value(Number n) { out.print(n.toString()) ; }

}
