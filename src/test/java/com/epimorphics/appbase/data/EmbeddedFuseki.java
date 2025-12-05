package com.epimorphics.appbase.data;

import com.epimorphics.appbase.data.impl.RemoteSparqlSource;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.vocabulary.RDF;

public class EmbeddedFuseki {
    protected static final String datasetPath = "/ds";

    protected FusekiServer server;
    protected int port;
    protected String serverBase;
    protected String tdbLocation;
    DatasetGraph dsg;

    public EmbeddedFuseki() {
        this(3333);
    }

    public EmbeddedFuseki(int port) {
        setPort(port);
    }

    public void setPort(int port) {
        this.port = port;
        serverBase = "http://localhost:" + port + datasetPath;
    }

    public void setTdbLocation(String location) {
        this.tdbLocation = location;
    }

    public void start() {
        dsg = DatasetGraphFactory.create();

        server = FusekiServer.create()
                .port(port)
                .add(datasetPath, dsg)
                .loopback(true)
                .build();

        server.start();
        waitForServer();
    }

    protected void waitForServer() {
        SparqlSource source = getAsSource();
        for (int i = 0; i < 100; i++) {
            try {
                source.describeAll(RDF.nil.getURI());
                return;
            } catch (Exception e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public String getServerBase() {
        return serverBase;
    }

    public String getUpdateEndpoint() {
        return serverBase + "/update";
    }

    public String getGraphEndpoint() {
        return serverBase + "/data";
    }

    public SparqlSource getAsSource() {
        RemoteSparqlSource source = new RemoteSparqlSource();
        source.setEndpoint( serverBase + "/query" );
        source.setGraphEndpoint( getGraphEndpoint() );
        source.setUpdateEndpoint( getUpdateEndpoint() );
        return source;
    }

}
