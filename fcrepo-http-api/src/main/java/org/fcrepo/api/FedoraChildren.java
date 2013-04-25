
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
@Path("/rest/fcr:children/{path: .*}")
public class FedoraChildren extends AbstractResource {

    private static final Logger logger = getLogger(FedoraChildren.class);

    @Autowired
    private ObjectService objectService;

    /**
     * 
     * Provides a serialized list of JCR names for all objects in the repo.
     * 
     * @return 200
     * @throws RepositoryException
     */
    @GET
    @Path("")
    public Response getObjects(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException {

        final String path = toPath(pathList);
        logger.info("getting children of {}", path);
        return ok(objectService.getObjectNames(path).toString()).build();

    }

    public ObjectService getObjectService() {
        return objectService;
    }

    
    public void setObjectService(ObjectService objectService) {
        this.objectService = objectService;
    }
    
}
