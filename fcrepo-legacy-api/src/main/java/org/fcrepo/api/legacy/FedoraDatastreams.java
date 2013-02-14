
package org.fcrepo.api.legacy;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;


import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.api.legacy.service.FedoraService;
import org.fcrepo.api.legacy.service.ObjectNotFoundException;
import org.fcrepo.jaxb.responses.DatastreamHistory;
import org.fcrepo.jaxb.responses.DatastreamProfile;
import org.fcrepo.jaxb.responses.ObjectDatastreams;

@Path("/objects/{pid}/datastreams")
public class FedoraDatastreams extends AbstractResource {

	final private FedoraService fedoraService = new FedoraService();

	@PostConstruct
	public void loginReadOnlySession() throws LoginException,
	RepositoryException {
		fedoraService.loginReadOnlySession();
	}

	@PreDestroy
	public void logoutReadOnlySession() {
		fedoraService.logoutReadOnlySession();
	}

	/**
	 * Returns a list of datastreams for the object
	 * 
	 * @param pid
	 *            persistent identifier of the digital object
	 * @return the list of datastreams
	 * @throws RepositoryException
	 * @throws IOException
	 * @throws TemplateException
	 */

	@GET
	@Path("/")
	@Produces({TEXT_XML, APPLICATION_JSON})
	public Response getDatastreams(@PathParam("pid")
	final String pid) throws RepositoryException, IOException {
		ObjectDatastreams objectDatastreams = fedoraService.getDatastreams(pid);
		if (objectDatastreams != null) {
			return ok(objectDatastreams).build();
		} else {
			return four04;
		}
	}

	/**
	 * Create a new datastream
	 * 
	 * @param pid
	 *            persistent identifier of the digital object
	 * @param dsid
	 *            datastream identifier
	 * @param contentType
	 *            Content-Type header
	 * @param requestBodyStream
	 *            Binary blob
	 * @return 201 Created
	 * @throws RepositoryException
	 * @throws IOException
	 */
	@POST
	@Path("/{dsid}")
	public Response addDatastream(@PathParam("pid")
	final String pid, @PathParam("dsid")
	final String dsid, @HeaderParam("Content-Type")
	MediaType contentType, InputStream requestBodyStream)
			throws RepositoryException, IOException {
		try {
			fedoraService.addDatastream(pid, dsid, contentType, requestBodyStream);
			return created(uriInfo.getAbsolutePath()).build();
		} catch (AccessControlException e) {
			return four03;
		} catch (ObjectNotFoundException e) {
			return notAcceptable(null).build();
		}
	}

	/**
	 * Modify an existing datastream's content
	 * 
	 * @param pid
	 *            persistent identifier of the digital object
	 * @param dsid
	 *            datastream identifier
	 * @param contentType
	 *            Content-Type header
	 * @param requestBodyStream
	 *            Binary blob
	 * @return 201 Created
	 * @throws RepositoryException
	 * @throws IOException
	 */
	@PUT
	@Path("/{dsid}")
	public Response modifyDatastream(@PathParam("pid")
	final String pid, @PathParam("dsid")
	final String dsid, @HeaderParam("Content-Type")
	MediaType contentType, InputStream requestBodyStream)
			throws RepositoryException, IOException {
		try {
			fedoraService.modifyDatastream(pid, dsid, contentType, requestBodyStream);
			return Response.created(uriInfo.getAbsolutePath()).build();
		} catch (AccessControlException e) {
			return four03;
		}
	}


	/**
	 * Get the datastream profile of a datastream
	 * 
	 * @param pid
	 *            persistent identifier of the digital object
	 * @param dsid
	 *            datastream identifier
	 * @return 200
	 * @throws RepositoryException
	 * @throws IOException
	 * @throws TemplateException
	 */
	@GET
	@Path("/{dsid}")
	@Produces({TEXT_XML, APPLICATION_JSON})
	public Response getDatastream(@PathParam("pid")
	final String pid, @PathParam("dsid")
	final String dsid) throws RepositoryException, IOException {
		try {
			final DatastreamProfile dsProfile =fedoraService.getDatastream(pid, dsid);
			return ok(dsProfile).build();
		} catch (ObjectNotFoundException e) {
			return four04;
		}
	}

	/**
	 * Get the binary content of a datastream
	 * 
	 * @param pid
	 *            persistent identifier of the digital object
	 * @param dsid
	 *            datastream identifier
	 * @return Binary blob
	 * @throws RepositoryException
	 */
	@GET
	@Path("/{dsid}/content")
	public Response getDatastreamContent(@PathParam("pid")
	final String pid, @PathParam("dsid")
	final String dsid) throws RepositoryException {
		try {
			Map<String,Object> content = fedoraService.getDatastreamContent(pid, dsid);
			return ok(content.get("stream"), (MediaType) content.get("mimeType")).build();
		} catch (ObjectNotFoundException e) {
			return four04;
		}
	}

	/**
	 * Get previous version information for this datastream
	 * 
	 * @param pid
	 *            persistent identifier of the digital object
	 * @param dsid
	 *            datastream identifier
	 * @return 200
	 * @throws RepositoryException
	 * @throws IOException
	 * @throws TemplateException
	 */
	@GET
	@Path("/{dsid}/versions")
	@Produces({TEXT_XML, APPLICATION_JSON})
	// TODO implement this after deciding on a versioning model
	public
	Response getDatastreamHistory(@PathParam("pid")
	final String pid, @PathParam("dsid")
	final String dsid) throws RepositoryException, IOException {
		try {
			final DatastreamHistory dsHistory = fedoraService.getDatastreamHistory(pid,dsid);
			return ok(dsHistory).build();
		} catch (ObjectNotFoundException e) {
			return four04;
		}
	}

	/**
	 * Get previous version information for this datastream. See
	 * /{dsid}/versions. Kept for compatibility with fcrepo <3.5 API.
	 * 
	 * @deprecated
	 * 
	 * @param pid
	 *            persistent identifier of the digital object
	 * @param dsid
	 *            datastream identifier
	 * @return 200
	 * @throws RepositoryException
	 * @throws IOException
	 * @throws TemplateException
	 */
	@GET
	@Path("/{dsid}/history")
	@Produces(TEXT_XML)
	@Deprecated
	public Response getDatastreamHistoryOld(@PathParam("pid")
	final String pid, @PathParam("dsid")
	final String dsid) throws RepositoryException, IOException {
		return getDatastreamHistory(pid, dsid);
	}

	/**
	 * Purge the datastream
	 * 
	 * @param pid
	 *            persistent identifier of the digital object
	 * @param dsid
	 *            datastream identifier
	 * @return 204
	 * @throws RepositoryException
	 */
	@DELETE
	@Path("/{dsid}")
	public Response deleteDatastream(@PathParam("pid")
	String pid, @PathParam("dsid")
	String dsid) throws RepositoryException {
		try { 
			fedoraService.deleteDatastream(pid, dsid);
			return noContent().build();
		} catch(AccessControlException e) {
			return four03;
		} catch (ObjectNotFoundException e) {
			return four04;
		}

	}


}
