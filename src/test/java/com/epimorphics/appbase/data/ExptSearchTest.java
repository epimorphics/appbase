/******************************************************************
 * File:        ExptSearchTest.java
 * Created by:  Dave Reynolds
 * Created on:  24 Feb 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.data;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;

/**
 * Experimental code investigating discrepancy between jena text index
 * config and the 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ExptSearchTest {

    public static void main(String[] args) {
        Dataset ds = DatasetFactory.assemble(
                "src/test/textSearch/expt/asm.ttl", 
                "http://localhost/jena_example/#text_dataset") ;
//        Dataset ds = TDBFactory.createDataset("target/DB");
        
        query(ds, "PREFIX text: <http://jena.apache.org/text#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT * WHERE {?x text:query 'someplace'; rdfs:label ?label}");
//        query(ds, "PREFIX text: <http://jena.apache.org/text#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT * WHERE {?x  rdfs:label ?label}");
    }
    
    public static void query(Dataset ds, String queryString) {
        QueryExecution qexec = QueryExecutionFactory.create(queryString, ds);
        try {
            ResultSet rs = qexec.execSelect();
            System.out.println( ResultSetFormatter.asText(rs) );
        } finally { 
            qexec.close();
        }
        
    }

}
