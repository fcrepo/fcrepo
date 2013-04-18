
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.jaxb.responses.access.ObjectProfile.ObjectStates.A;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraObject;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.services.ObjectService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/objects")
public class FedoraObjects extends AbstractResource {

    private static final Logger logger = getLogger(FedoraObjects.class);

    @Autowired
    ObjectService objectService;

    /**
     * 
     * Provides a serialized list of JCR names for all objects in the repo.
     * 
     * @return 200
     * @throws RepositoryException
     */
    @GET
    public Response getObjects() throws RepositoryException {

        return ok(objectService.getObjectNames().toString()).build();

    }

    /**
     * Creates a new object with a repo-chosen PID
     * 
     * @return 201
     * @throws RepositoryException
     */
    @POST
    @Path("/new")
    public Response ingestAndMint() throws RepositoryException {
        return ingest(pidMinter.mintPid(), "");
    }

    /**
     * Does nothing yet-- must be improved to handle the FCREPO3 PUT to /objects/{pid}
     * 
     * @param pid
     * @return 201
     * @throws RepositoryException
     */
    @PUT
    @Path("/{pid}")
    public Response modify(@PathParam("pid")
    final String pid) throws RepositoryException {
        final Session session = getAuthenticatedSession();
        try {
            // TODO do something with awful mess of fcrepo3 query params
            session.save();
            return created(uriInfo.getRequestUri()).build();
        } finally {
            session.logout();
        }
    }

    /**
     * Creates a new object.
     * 
     * @param pid
     * @return 201
     * @throws RepositoryException
     */
    @POST
    @Path("/{pid}")
    public Response ingest(@PathParam("pid")
    final String pid, @QueryParam("label")
    @DefaultValue("")
    final String label) throws RepositoryException {

        logger.debug("Attempting to ingest with pid: {}", pid);

        final Session session = getAuthenticatedSession();
        try {
            final FedoraObject result =
                    objectService.createObject(session, pid);
            if (label != null && !"".equals(label)) {
                result.setLabel(label);
            }
            session.save();
            logger.debug("Finished ingest with pid: {}", pid);
            return created(uriInfo.getRequestUri()).entity(pid).build();

        } finally {
            session.logout();
        }
    }

    /**
     * Returns an object profile.
     * 
     * @param pid
     * @return 200
     * @throws RepositoryException
     * @throws IOException
     */
    @GET
    @Path("/{pid}")
    @Produces({TEXT_XML, APPLICATION_JSON, TEXT_HTML})
    public ObjectProfile getObject(@PathParam("pid")
    final String pid) throws RepositoryException, IOException {

        final ObjectProfile objectProfile = new ObjectProfile();
        final FedoraObject obj = objectService.getObject(pid);
        objectProfile.pid = pid;
        objectProfile.objLabel = obj.getLabel();
        objectProfile.objOwnerId = obj.getOwnerId();
        objectProfile.objCreateDate = obj.getCreated();
        objectProfile.objLastModDate = obj.getLastModified();
        objectProfile.objSize = obj.getSize();
        objectProfile.objItemIndexViewURL =
                uriInfo.getAbsolutePathBuilder().path("datastreams").build();
        objectProfile.objState = A;
        objectProfile.objModels = obj.getModels();
        return objectProfile;

    }

    /**
     * Deletes an object.
     * 
     * @param pid
     * @return
     * @throws RepositoryException
     */
    @DELETE
    @Path("/{pid}")
    public Response deleteObject(@PathParam("pid")
    final String pid) throws RepositoryException {
        final Session session = getAuthenticatedSession();
        objectService.deleteObject(pid, session);
        session.save();
        return noContent().build();
    }

}
