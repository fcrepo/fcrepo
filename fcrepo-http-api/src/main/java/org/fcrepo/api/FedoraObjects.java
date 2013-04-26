
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/{path: .*}")
public class FedoraObjects extends AbstractResource {

    private static final Logger logger = getLogger(FedoraObjects.class);

    /**
     * Does nothing yet-- must be improved to handle the FCREPO3 PUT to /objects/{pid}
     * 
     * @param pid
     * @return 201
     * @throws RepositoryException
     */
    @PUT
    public Response modifyObject(@PathParam("path")
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
     * @throws InvalidChecksumException 
     * @throws IOException 
     */
    @POST
    public Response createObject(
            @PathParam("path") final List<PathSegment> pathList,
            @QueryParam("label") @DefaultValue("") final String label,
            @QueryParam("mixin") @DefaultValue(FedoraJcrTypes.FEDORA_OBJECT) String mixin,
            @QueryParam("checksumType") final String checksumType,
            @QueryParam("checksum") final String checksum,
            @HeaderParam("Content-Type") final MediaType requestContentType,
            final InputStream requestBodyStream
            ) throws RepositoryException, IOException, InvalidChecksumException {
        
        String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        final Session session = getAuthenticatedSession();
        
        try {
            if (objectService.exists(path)) {
                return Response.status(HttpStatus.SC_CONFLICT).entity(path + " is an existing resource").build();
            }
            if (FedoraJcrTypes.FEDORA_OBJECT.equals(mixin)){
                final FedoraObject result =
                        objectService.createObject(session, path);
                if (label != null && !"".equals(label)) {
                    result.setLabel(label);
                }
            }
            if (FedoraJcrTypes.FEDORA_DATASTREAM.equals(mixin)){
                final MediaType contentType =
                        requestContentType != null ? requestContentType
                                : APPLICATION_OCTET_STREAM_TYPE;
                final Node result =
                datastreamService.createDatastreamNode(session, path, contentType
                        .toString(), requestBodyStream, checksumType, checksum);
                Datastream ds = new Datastream(result);
                ds.setLabel(label);
            }
            session.save();
            logger.debug("Finished creating {} with path: {}", mixin, path);
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
    @Produces({TEXT_XML, APPLICATION_JSON, TEXT_HTML})
    public Response getObjects(
            @PathParam("path") final List<PathSegment> pathList,
            @QueryParam("mixin") @DefaultValue("") String mixin
            ) throws RepositoryException, IOException {

        final String path = toPath(pathList);
        logger.info("getting children of {}", path);
        if ("".equals(mixin)) {
            mixin = null;
        }
        else if (FedoraJcrTypes.FEDORA_OBJECT.equals(mixin)) {
            mixin = "nt:folder";
        } else if (FedoraJcrTypes.FEDORA_DATASTREAM.equals(mixin)) {
            mixin = "nt:file";
        }
        return ok(objectService.getObjectNames(path, mixin).toString()).build();

    }

    /**
     * Deletes an object.
     * 
     * @param pid
     * @return
     * @throws RepositoryException
     */
    @DELETE
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

    public DatastreamService getDatastreamService() {
        return datastreamService;
    }

    
    public void setDatastreamService(DatastreamService datastreamService) {
        this.datastreamService = datastreamService;
    }

}
