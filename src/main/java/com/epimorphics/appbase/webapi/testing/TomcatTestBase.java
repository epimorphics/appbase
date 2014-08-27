/******************************************************************
 * File:        TomcatTestContainerFactory.java
 * Created by:  Dave Reynolds
 * Created on:  30 Nov 2012
 *
 * (c) Copyright 2012, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.appbase.webapi.testing;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.catalina.startup.Tomcat;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.junit.After;
import org.junit.Before;

import com.epimorphics.util.TestUtil;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public abstract class TomcatTestBase {

    protected static final String BASE_URL = "http://localhost:8070/";

    protected Tomcat tomcat ;
    protected Client c;

    abstract public String getWebappRoot() ;
    
    public String getWebappContext() {
        return "/";
    }


    @Before
    public void containerStart() throws Exception {
        String root = getWebappRoot();
        tomcat = new Tomcat();
        tomcat.setPort(8070);

        tomcat.setBaseDir(".");

        String contextPath = getWebappContext();

        File rootF = new File(root);
        if (!rootF.exists()) {
            rootF = new File(".");
        }
        if (!rootF.exists()) {
            System.err.println("Can't find root app: " + root);
            System.exit(1);
        }

        tomcat.addWebapp(contextPath,  rootF.getAbsolutePath());
        tomcat.start();

        // Allow arbitrary HTTP methods so we can use PATCH
        DefaultClientConfig config = new DefaultClientConfig();
        config.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
        c = Client.create(config);

        checkLive(200);
    }

    @After
    public void containerStop() throws Exception {
        tomcat.stop();
        tomcat.destroy();
        try {
            checkLive(503);
        } catch (Throwable e) {
            // Can get net connection exceptions talking to dead tomcat, that's OK
        }
    }

    protected int postFileStatus(String file, String uri) {
        return postFileStatus(file, uri, "text/turtle");
    }

    protected int postFileStatus(String file, String uri, String mime) {
        return postFile(file, uri, mime).getStatus();
    }

    protected ClientResponse postFile(String file, String uri) {
        return postFile(file, uri, "text/turtle");
    }

    protected ClientResponse postFile(String file, String uri, String mime) {
        WebResource r = c.resource(uri);
        File src = new File(file);
        ClientResponse response = r.type(mime).post(ClientResponse.class, src);
        return response;
    }

    protected ClientResponse postModel(Model m, String uri) {
        WebResource r = c.resource(uri);
        StringWriter sw = new StringWriter();
        m.write(sw, "Turtle");
        ClientResponse response = r.type("text/turtle").post(ClientResponse.class, sw.getBuffer().toString());
        return response;
    }

    protected ClientResponse invoke(String method, String file, String uri, String mime) {
        WebResource r = c.resource(uri);
        ClientResponse response = null;
        if (file == null) {
            response = r.type(mime).header("X-HTTP-Method-Override", method).post(ClientResponse.class);
        } else {
            File src = new File(file);
            response = r.type(mime).header("X-HTTP-Method-Override", method).post(ClientResponse.class, src);
        }
        return response;
    }

    protected ClientResponse post(String uri, String...paramvals) {
        WebResource r = c.resource(uri);
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            r = r.queryParam(param, value);
        }
        ClientResponse response = r.post(ClientResponse.class);
        return response;
    }

    protected ClientResponse postForm(String uri, String...paramvals) {
        WebResource r = c.resource(uri);
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            formData.add(param, value);
        }
        ClientResponse response = r.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, formData);
        return response;
    }

    protected ClientResponse invoke(String method, String file, String uri) {
        return invoke(method, file, uri, "text/turtle");
    }

    protected Model getModelResponse(String uri, String...paramvals) {
        WebResource r = c.resource( uri );
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            r = r.queryParam(param, value);
        }
        InputStream response = r.accept("text/turtle").get(InputStream.class);
        Model result = ModelFactory.createDefaultModel();
        result.read(response, uri, "Turtle");
        return result;
    }

    protected ClientResponse getResponse(String uri) {
        return getResponse(uri, "text/turtle");
    }

    protected ClientResponse getResponse(String uri, String mime) {
        WebResource r = c.resource( uri );
        return r.accept(mime).get(ClientResponse.class);
    }
    
    protected JsonObject getJSONResponse(String uri) {
        ClientResponse r = getResponse(uri, MediaType.APPLICATION_JSON);
        return JSON.parse( r.getEntityInputStream() );
    }


    protected Model checkModelResponse(String fetch, String rooturi, String file, Property...omit) {
        Model m = getModelResponse(fetch);
        Resource actual = m.getResource(rooturi);
        Resource expected = FileManager.get().loadModel(file).getResource(rooturi);
        assertTrue(expected.listProperties().hasNext());  // guard against wrong rooturi in config
        TestUtil.testResourcesMatch(expected, actual, omit);
        return m;
    }

    protected Model checkModelResponse(Model m, String rooturi, String file, Property...omit) {
        Resource actual = m.getResource(rooturi);
        Resource expected = FileManager.get().loadModel(file).getResource(rooturi);
        assertTrue(expected.listProperties().hasNext());  // guard against wrong rooturi in config
        TestUtil.testResourcesMatch(expected, actual, omit);
        return m;
    }

    protected Model checkModelResponse(Model m, String file, Property...omit) {
        Model expected = FileManager.get().loadModel(file);
        for (Resource root : expected.listSubjects().toList()) {
            if (root.isURIResource()) {
                TestUtil.testResourcesMatch(root, m.getResource(root.getURI()), omit);
            }
        }
        return m;
    }

    protected void printStatus(ClientResponse response) {
        String msg = "Response: " + response.getStatus();
        if (response.hasEntity() && response.getStatus() != 204) {
            msg += " (" + response.getEntity(String.class) + ")";
        }
        System.out.println(msg);
    }


    protected void checkLive(int targetStatus) {
        boolean tomcatLive = false;
        int count = 0;
        while (!tomcatLive) {
            int status = getResponse(BASE_URL).getStatus();
            if (status != targetStatus) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (count++ > 10) {
                    assertTrue("Too many tries", false);
                }
            } else {
                tomcatLive = true;
            }
        }
    }

}
