/******************************************************************
 * File:        GenericJSONReader.java
 * Created by:  Dave Reynolds
 * Created on:  8 Aug 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.server.webapi.marshalling;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;

import com.epimorphics.appbase.webapi.WebApiException;

@Provider
@Consumes("application/json")
public class GenericJSONReader implements MessageBodyReader<JsonObject> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return type.equals( JsonObject.class );
    }

    @Override
    public JsonObject readFrom(Class<JsonObject> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        
        try {
            return JSON.parse(entityStream);
        } catch (Exception e) {
            throw new WebApiException(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

}
