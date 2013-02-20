
package org.fcrepo.api.legacy;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import java.io.IOException;
import java.security.AccessControlException;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.api.legacy.service.FedoraService;
import org.fcrepo.api.legacy.service.ObjectNotFoundException;
import org.fcrepo.jaxb.responses.ObjectProfile;

@Path("/objects")
public class FedoraObjects extends AbstractResource {
	@Inject
	private FedoraService fedoraService;


    @GET
    public Response getObjects() throws RepositoryException {
    	String nodes = fedoraService.getObjects();
        return ok(nodes).build();

    }

    @POST
    @Path("/new")
    public Response ingestAndMint() throws RepositoryException {
        return ingest(pidMinter.mintPid());
    }

    @PUT
    @Path("/{pid}")
    @Consumes({TEXT_XML, APPLICATION_JSON})
    public Response modify(@PathParam("pid")
    final String pid, final ObjectProfile objProfile)
    		throws RepositoryException {
    	try {
    		fedoraService.modify(pid, objProfile);
        	return created(uriInfo.getAbsolutePath()).build();
    	} catch (ObjectNotFoundException e) {
    		return status(CONFLICT).entity("No such object").build();

    	}
    }

    @POST
    @Path("/{pid}")
    public Response ingest(@PathParam("pid")
    final String pid) throws RepositoryException {
    	try {
    		fedoraService.ingest(pid);
    		return created(uriInfo.getAbsolutePath()).entity(pid).build();
    	} catch (AccessControlException e) {
    		return four03;
    	}
    }

    @GET
    @Path("/{pid}")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public Response getObject(@PathParam("pid")
    final String pid) throws RepositoryException, IOException {
    	
    	try {
    		ObjectProfile objectProfile = fedoraService.getObject(pid, uriInfo.getAbsolutePathBuilder().path("datastreams")
                .build());
        	return ok(objectProfile).build();
        } catch (ObjectNotFoundException e) {
            return four04;
        }
    }

    @DELETE
    @Path("/{pid}")
    public Response deleteObject(@PathParam("pid")
    final String pid) throws RepositoryException {
    	try {
    		fedoraService.deleteObject(pid);
    		return noContent().build();
    	} catch (AccessControlException e) {
    		return four03;
    	}

    }


}
