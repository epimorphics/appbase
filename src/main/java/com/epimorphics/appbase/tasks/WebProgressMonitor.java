/******************************************************************
 * File:        WebProgressReporter.java
 * Created by:  Dave Reynolds
 * Created on:  16 Oct 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.appbase.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.ws.rs.core.MediaType;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;

import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.ProgressMonitor;
import com.epimorphics.tasks.TaskState;
import com.epimorphics.util.EpiException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class WebProgressMonitor implements ProgressMonitor {
    public static final String PROGRESS_FIELD = "progress";
    public static final String STATE_FIELD    = "state";
    public static final String SUCEEDED_FIELD = "succeeded";
    public static final String MESSAGES_FIELD = "messages";

    protected JsonObject lastStatus;
    protected List<ProgressMessage> messages;
    protected WebResource resource;
    
    public WebProgressMonitor(String url) {
        resource = new Client().resource(url);
    }
    
    /**
     * Fetch an up to date version of the status information from the server.
     * Throws runtime exception if the web resource responds with an error
     */
    public void update() {
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            throw new EpiException("Status resource fetch failed: " + response.getStatus() + " " + response.getEntity(String.class));
        }
        InputStream in = response.getEntityInputStream();
        lastStatus = JSON.parse( in );
        try {
            in.close();
        } catch (IOException e) {
            // ignore
        }
        messages = null;
    }
    
    public void waitForTermination() {
        while( getState() != TaskState.Terminated ) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore
            }
            update();
        }
    }
    
    protected List<ProgressMessage> getMessageList() {
        if (messages == null) {
            JsonArray messageArray = getSafeField(MESSAGES_FIELD).getAsArray();
            messages = new ArrayList<>( messageArray.size() );
            for (ListIterator<JsonValue> i = messageArray.listIterator(); i.hasNext();) {
                messages.add( new ProgressMessage( i.next().getAsObject() ) );
            }
        }
        return messages;
    }
    
    private JsonValue getSafeField(String field) {
        if (lastStatus == null) {
            update();
        }
        JsonValue val = lastStatus.get(field);
        if (val == null) {
            throw new EpiException("Could not find field " + field + " in status response");
        }
        return val;
    }
    
    @Override
    public TaskState getState() {
        return TaskState.valueOf( getSafeField(STATE_FIELD).getAsString().value() );
    }

    @Override
    public int getProgress() {
        return getSafeField(PROGRESS_FIELD).getAsNumber().value().intValue();
    }

    @Override
    public boolean succeeded() {
        return getSafeField(SUCEEDED_FIELD).getAsBoolean().value();
    }

    @Override
    public List<ProgressMessage> getMessages() {
        return getMessageList();
    }

    @Override
    public synchronized List<ProgressMessage> getMessagesSince(int offset) {
        List<ProgressMessage> m = getMessageList();
        return m.subList(offset, m.size());
    }

    @Override
    public synchronized boolean moreMessagesSince(int offset) {
        return getMessageList().size() > offset;
    }

}
