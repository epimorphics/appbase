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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.After;
import org.junit.Before;

import com.epimorphics.util.NameUtils;
import com.epimorphics.util.TestUtil;

public abstract class TomcatTestBase {

    protected static final String BASE_URL = "http://localhost:8070/";

    protected Tomcat tomcat ;
    protected Client c;

    abstract public String getWebappRoot() ;
    
    public String getWebappContext() {
        return "/";
    }
    
    /**
     * URL to use for liveness tests
     */
    public String getTestURL() {
        return NameUtils.ensureLastSlash( BASE_URL.substring(0, BASE_URL.length()-1) + getWebappContext() );
    }

    protected void configureContext(Context context) {
        // do nothing by default
    }

    @Before
    public void containerStart() throws Exception {
        String root = getWebappRoot();
        tomcat = new Tomcat();
        tomcat.setPort(8070);
        tomcat.getConnector();

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

        Context context = tomcat.addWebapp(contextPath,  rootF.getAbsolutePath());
        configureContext(context);
        tomcat.start();

        // Allow arbitrary HTTP methods so we can use PATCH
        ClientConfig config = new ClientConfig();
        // TODO Not sure this is correct or needed any more
        // config.getProperties().put(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        c = ClientBuilder.newClient(config);

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

    protected Response postFile(String file, String uri) {
        return postFile(file, uri, "text/turtle");
    }

    protected Response postFile(String file, String uri, String mime) {
        WebTarget r = c.target(uri);
        File src = new File(file);
        Response response = r.request().post( Entity.entity(src, mime) );
        return response;
    }

    protected Response postModel(Model m, String uri) {
        WebTarget r = c.target(uri);
        StringWriter sw = new StringWriter();
        m.write(sw, "Turtle");
        Response response = r.request().post(Entity.entity(sw.getBuffer().toString(), "text/turtle"));
        return response;
    }

    protected Response invoke(String method, String file, String uri, String mime) {
        WebTarget r = c.target(uri);
        Response response = null;
        if (file == null) {
            response = r.request().header("X-HTTP-Method-Override", method).post(Entity.entity(null, mime));
        } else {
            File src = new File(file);
            response = r.request().header("X-HTTP-Method-Override", method).post(Entity.entity(src, mime));
        }
        return response;
    }

    protected Response post(String uri, String...paramvals) {
        WebTarget r = c.target(uri);
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            r = r.queryParam(param, value);
        }
        Response response = r.request().post(null);
        return response;
    }

    protected Response postForm(String uri, String...paramvals) {
        WebTarget r = c.target(uri);
        Form form = new Form();

        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            form.param(param, value);
        }
        Response response = r.request().post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        return response;
    }

    protected Response invoke(String method, String file, String uri) {
        return invoke(method, file, uri, "text/turtle");
    }

    protected Model getModelResponse(String uri, String...paramvals) {
        WebTarget r = c.target( uri );
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            r = r.queryParam(param, value);
        }
        InputStream response = r.request("text/turtle").get(InputStream.class);
        Model result = ModelFactory.createDefaultModel();
        result.read(response, uri, "Turtle");
        return result;
    }

    protected Response getResponse(String uri) {
        return getResponse(uri, "text/turtle");
    }

    protected Response getResponse(String uri, String mime) {
        WebTarget r = c.target( uri );
        return r.request(mime).get();
    }
    
    protected JsonObject getJSONResponse(String uri) {
        Response r = getResponse(uri, MediaType.APPLICATION_JSON);
        return JSON.parse( r.readEntity(InputStream.class) );
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

    protected void printStatus(Response response) {
        String msg = "Response: " + response.getStatus();
        if (response.hasEntity() && response.getStatus() != 204) {
            msg += " (" + response.readEntity(String.class) + ")";
        }
        System.out.println(msg);
    }


    protected void checkLive(int targetStatus) {
        boolean tomcatLive = false;
        int count = 0;
        while (!tomcatLive) {
            int status = getResponse( getTestURL() ).getStatus();
            if (status != targetStatus) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    assertTrue("Interrupted", false);
                }
                if (count++ > 120 ) {
                    assertTrue("Too many tries", false);
                }
            } else {
                tomcatLive = true;
            }
        }
    }

}
