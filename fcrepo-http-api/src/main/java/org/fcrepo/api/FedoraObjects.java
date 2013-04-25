
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
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraObject;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.services.ObjectService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
@Path("/rest/{path: .*}")
public class FedoraObjects extends AbstractResource {

    private static final Logger logger = getLogger(FedoraObjects.class);

    @Autowired
    private ObjectService objectService;

    /**
     * Does nothing yet-- must be improved to handle the FCREPO3 PUT to /objects/{pid}
     * 
     * @param pid
     * @return 201
     * @throws RepositoryException
     */
    @PUT
    @Path("")
    public Response modify(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException {
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
    @Path("")
    public Response ingest(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("label")
    @DefaultValue("")
    final String label) throws RepositoryException {
        
        String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        final Session session = getAuthenticatedSession();
        
        try {
            final FedoraObject result =
                    objectService.createObject(session, path);
            if (label != null && !"".equals(label)) {
                result.setLabel(label);
            }
            session.save();
            logger.debug("Finished ingest with path: {}", path);
            return created(uriInfo.getRequestUri()).entity(path).build();

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
    @Path("")
    @Produces({TEXT_XML, APPLICATION_JSON, TEXT_HTML})
    public ObjectProfile getObject(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException, IOException {

        final String path = toPath(pathList);
        logger.trace("getting object profile {}", path);
        final ObjectProfile objectProfile = new ObjectProfile();
        final FedoraObject obj = objectService.getObjectByPath(path);
        objectProfile.pid = obj.getName();
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
    @Path("")
    public Response deleteObject(@PathParam("path")
    final List<PathSegment> path) throws RepositoryException {
        final Session session = getAuthenticatedSession();
        objectService.deleteObjectByPath(toPath(path), session);
        session.save();
        return noContent().build();
    }

    
    public ObjectService getObjectService() {
        return objectService;
    }

    
    public void setObjectService(ObjectService objectService) {
        this.objectService = objectService;
    }
    
}
