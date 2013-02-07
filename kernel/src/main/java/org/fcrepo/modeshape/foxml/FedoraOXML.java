package org.fcrepo.modeshape.foxml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.fcrepo.modeshape.AbstractResource;

import com.google.common.collect.ImmutableMap;

@Path("/foxml")
public class FedoraOXML extends AbstractResource {

	//private final Logger logger = Logger.getLogger(FedoraOXML.class);

	@PUT
	@Path("/{filename}")
	@Consumes("text/xml")
	public Response addFOXML(@PathParam("filename") final String filename,
			InputStream foxml) throws RepositoryException, IOException {

		final Session session = repo.login();
		if (session.hasPermission("/foxml", "add_node")) {
			final String foxmlpath = "/foxml/" + filename;
			jcrTools.uploadFile(session, foxmlpath, foxml);
			session.save();
			session.logout();
			return Response.created(URI.create(foxmlpath)).build();
		} else {
			session.logout();
			return four03;
		}
	}

	@GET
	@Path("/{filename}")
	public Response getFOXML(@PathParam("filename") final String filename)
			throws RepositoryException {

		final String foxmlpath = "/foxml" + filename;

		final Session session = repo.login();

		if (session.nodeExists(foxmlpath)) {
			final Node foxmlfile = session.getNode(foxmlpath);
			InputStream contentStream = foxmlfile.getNode("jcr:content")
					.getProperty("jcr:data").getBinary().getStream();
			session.logout();
			return Response.ok(contentStream, "text/xml").build();
		} else {
			session.logout();
			return four04;
		}
	}

	@GET
	@Path("/")
	public Response getFOXMLs() throws RepositoryException,
			JsonGenerationException, JsonMappingException, IOException {

		final Session session = repo.login();

		final Node foxml = session.getNode("/foxml");

		ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
		for (NodeIterator i = foxml.getNodes(); i.hasNext();) {
			Node n = i.nextNode();
			b.put(n.getName(), n.getPath());
		}
		String foxmls = mapper.writerWithType(Map.class).writeValueAsString(
				b.build());
		session.logout();
		return Response.ok().entity(foxmls).build();

	}
}
