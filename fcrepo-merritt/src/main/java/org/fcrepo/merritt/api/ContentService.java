package org.fcrepo.merritt.api;


import org.apache.cxf.message.Attachment;
import org.fcrepo.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static javax.ws.rs.core.Response.*;
import static org.modeshape.jcr.api.JcrConstants.*;

@Path("/content/{node}/{object}")
public class ContentService extends AbstractResource {

    final private Logger logger = LoggerFactory
            .getLogger(ContentService.class);

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
    public Response addVersion(@PathParam("object") final String object_id,
                               List<Attachment> atts) throws RepositoryException, IOException {
        final Session session = repo.login();
        final String objPath = "/objects/" + object_id;

        boolean created = false;
        if (!session.nodeExists(objPath)) {
            created = true;
            final Node obj = jcrTools.findOrCreateNode(session, objPath, NT_RESOURCE);
            obj.addMixin("fedora:object");
        }

        if (session.hasPermission(objPath, "add_node")) {

            for(Attachment a : atts) {

                final String dsPath = objPath + "/" + a.getId();
                if (session.hasPermission(dsPath, "add_node")) {

                    final Node ds = jcrTools.findOrCreateNode(session, dsPath, NT_FILE);
                    ds.addMixin("fedora:datastream");
                    final Node contentNode = jcrTools.findOrCreateChild(ds, JCR_CONTENT,
                            NT_RESOURCE);

                    Property dataProperty = contentNode.setProperty(JCR_DATA, session
                            .getValueFactory().createBinary(a.getDataHandler().getInputStream()));

                }  else {
                    session.logout();
                    return four03;
                }

            }
            session.logout();
            return created(uriInfo.getAbsolutePath()).build();
        } else {
            session.logout();
            return four03;
        }

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
