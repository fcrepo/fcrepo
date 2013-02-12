package org.fcrepo.merritt.api;


import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.fcrepo.AbstractResource;
import org.fcrepo.merritt.checkm.Entry;
import org.fcrepo.merritt.checkm.Reader;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static javax.ws.rs.core.Response.status;
import static org.modeshape.jcr.api.JcrConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

@Path("/content/{node}/{object}")
public class ContentService extends AbstractResource {

    public static final MediaType TEXT_CHECKM = new MediaType("text", "checkm");
    final private Logger logger = LoggerFactory
            .getLogger(ContentService.class);

    final private ObjectService objectService =  new ObjectService();
    final private DatastreamService datastreamService =  new DatastreamService();

    protected static HttpClient client;
    protected static final PoolingClientConnectionManager connectionManager =
            new PoolingClientConnectionManager();

    static {
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(5);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
        client = new DefaultHttpClient(connectionManager);
    }

    public static final String MANIFEST_PATH = "MANIFEST";

    @GET
    public Response getObjectContent(@PathParam("object") final String object_id) throws RepositoryException {
        final Session session = repo.login();
        final String dsPath = "/objects/" + object_id;

        if (session.nodeExists(dsPath)) {
            final Node object = session.getNode(dsPath);
            session.logout();
            return ok("").build();
        } else {
            session.logout();
            return four04;
        }
    }

    @POST
    @Consumes("multipart/form-data")
    public Response addVersion(@PathParam("object") final String object_id,
                               @Multipart(value = "manifest", type = "text/checkm", required = false) InputStream manifest,
                               @Multipart(value = "url", required = false) InputStream url
                               ) throws RepositoryException, IOException {
        final Session session = repo.login();
        final String objPath = "/objects/" + object_id;

        if(manifest == null && url == null) {
            return status(UNSUPPORTED_MEDIA_TYPE).build();
        }

        boolean created = false;
        if (!session.nodeExists(objPath)) {
            objectService.createObjectNode(session, objPath);
        }

        if(manifest != null) {
            datastreamService.createDatastreamNode(session, objPath + "/" + MANIFEST_PATH, TEXT_CHECKM, manifest);
        }

        if(url != null) {
            final HttpGet manifestRequest =
                    new HttpGet(IOUtils.toString(url, "UTF-8"));
            HttpResponse response = client.execute(manifestRequest);


            datastreamService.createDatastreamNode(session, objPath + "/" + MANIFEST_PATH, TEXT_CHECKM, response.getEntity().getContent());
        }

        Node manifest_node = session.getNode(objPath + "/" + MANIFEST_PATH);

        final InputStream responseStream =
                manifest_node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
                        .getStream();

        Reader manifestReader = new Reader(responseStream);

        List<Entry> entries = manifestReader.getEntries();

        for(Entry e : entries) {
            final InputStream sis = e.getSourceInputStream();
            datastreamService.createDatastreamNode(session, objPath + e.fileName, APPLICATION_OCTET_STREAM_TYPE, sis);
        }


        session.logout();
        return created(uriInfo.getAbsolutePath()).build();

    }

    @DELETE
    public Response deleteObject(@PathParam("object") final String object_id) throws RepositoryException {
        final Session session = repo.login();
        final String dsPath = "/objects/" + object_id;

        if (!session.nodeExists(dsPath)) {
            logger.debug("Tried to create a datastream for an object that doesn't exist, at resource path: "
                    + dsPath);
            return notAcceptable(null).build();
        }

        if (session.hasPermission(dsPath, "remove")) {
            final Node object = session.getNode(dsPath);
            object.remove();
            session.save();
            session.logout();
            return noContent().build();
        } else {
            return four03;
        }
    }

    @GET
    @Path("{version : \\d+ }")
    public Response getObjectVersionContent(@PathParam("object") final String object_id) throws RepositoryException {
        final Session session = repo.login();
        final String dsPath = "/objects/" + object_id;

        if (session.nodeExists(dsPath)) {
            final Node object = session.getNode(dsPath);
            session.logout();
            return ok("").build();
        } else {
            session.logout();
            return four04;
        }
    }

    @DELETE
    @Path("{version : \\d+ }")
    public Response deleteObjectVersion() {
        return ok("").build();
    }

    @GET
    @Path("{file : [a-zA-Z][a-zA-Z_0-9-]+}")
    public Response getObjectCurrentVersionFileContent(@PathParam("object") final String object_id, @PathParam("file") final String file_id) throws RepositoryException {
        final Session session = repo.login();
        final String dsPath = "/objects/" + object_id + "/" + file_id;

        if (session.nodeExists(dsPath)) {
            final Node datastream = session.getNode(dsPath);

            final String mimeType = datastream.hasProperty("fedora:contentType") ? datastream
                    .getProperty("fedora:contentType").getString()
                    : "application/octet-stream";

            final InputStream responseStream = datastream.getNode(JCR_CONTENT)
                    .getProperty(JCR_DATA).getBinary().getStream();

            session.logout();
            return ok(responseStream, mimeType).build();
        } else {
            session.logout();
            return four04;
        }
    }

    @GET
    @Path("{version : \\d+ }/{file : [a-zA-Z][a-zA-Z_0-9-]+}")
    public Response getObjectVersionFileContent(@PathParam("object") final String object_id, @PathParam("file") final String file_id) throws RepositoryException {
        final Session session = repo.login();
        final String dsPath = "/objects/" + object_id + "/" + file_id;

        if (session.nodeExists(dsPath)) {
            final Node datastream = session.getNode(dsPath);

            final String mimeType = datastream.hasProperty("fedora:contentType") ? datastream
                    .getProperty("fedora:contentType").getString()
                    : "application/octet-stream";

            final InputStream responseStream = datastream.getNode(JCR_CONTENT)
                    .getProperty(JCR_DATA).getBinary().getStream();

            session.logout();
            return ok(responseStream, mimeType).build();
        } else {
            session.logout();
            return four04;
        }
    }
}
