package org.fcrepo.modeshape;

import static com.google.common.collect.ImmutableSet.builder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.fcrepo.modeshape.jaxb.responses.NamespaceListing;
import org.fcrepo.modeshape.jaxb.responses.NamespaceListing.Namespace;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * The purpose of this class is to allow clients to manipulate the JCR
 * namespaces in play in a repository. This is necessary to allow the use of
 * traditional Fedora namespaced PIDs. Unlike Fedora Classic, a JCR requires
 * that namespaces be registered before use. The catalog of namespaces is very
 * simple, just a set of prefix-URI pairs.
 * 
 * @author ajs6f
 * 
 */
@Path("/namespaces")
public class FedoraNamespaces extends AbstractResource {

	@POST
	@Path("/{ns}")
	public Response registerObjectNamespace(@PathParam("ns") final String ns)
			throws RepositoryException {

		final Session session = repo.login();
		final NamespaceRegistry r = session.getWorkspace()
				.getNamespaceRegistry();
		r.registerNamespace(ns, "info:fedora/" + ns);
		session.logout();
		return Response.ok().entity(ns).build();
	}

	@GET
	@Path("/{ns}")
	@Produces(APPLICATION_JSON)
	public Response retrieveObjectNamespace(@PathParam("ns") final String prefix)
			throws RepositoryException {

		final Session session = repo.login();
		final NamespaceRegistry r = session.getWorkspace()
				.getNamespaceRegistry();

		if (ImmutableSet.copyOf(r.getPrefixes()).contains(prefix)) {
			session.logout();
			return ok("{ \"" + prefix + "\":\"" + r.getURI(prefix) + "\" }")
					.build();
		} else {
			session.logout();
			return four04;
		}
	}

	@POST
	@Path("")
	@Consumes(APPLICATION_JSON)
	public Response registerObjectNamespaceJSON(final InputStream message)
			throws RepositoryException, JsonParseException,
			JsonMappingException, IOException {

		final Session session = repo.login();
		final NamespaceRegistry r = session.getWorkspace()
				.getNamespaceRegistry();

		@SuppressWarnings("unchecked")
		final Map<String, String> nses = mapper.readValue(message, Map.class);
		for (final Map.Entry<String, String> entry : nses.entrySet()) {
			r.registerNamespace(entry.getKey(), entry.getValue());
		}
		session.logout();
		return ok(nses).build();
	}

	@GET
	@Path("")
	@Produces(TEXT_PLAIN)
	public Response getObjectNamespaces() throws RepositoryException {

		final Session session = repo.login();
		final NamespaceRegistry r = session.getWorkspace()
				.getNamespaceRegistry();

		StringBuffer out = new StringBuffer();
		String[] uris = r.getURIs();
		String[] prefixes = r.getPrefixes();
		for (int i = 0; i < uris.length; i++) {
			out.append(prefixes[i] + " : " + uris[i] + "\n");
		}
		session.logout();
		return ok(out.toString()).build();
	}

	@GET
	@Path("")
	@Produces({ TEXT_XML, APPLICATION_JSON })
	public NamespaceListing getObjectNamespacesInXML()
			throws RepositoryException, IOException {

		final Session session = repo.login();
		final NamespaceRegistry r = session.getWorkspace()
				.getNamespaceRegistry();
		final Builder<Namespace> b = builder();
		for (final String prefix : r.getPrefixes()) {
			b.add(new Namespace(prefix, URI.create(r.getURI(prefix))));
		}
		session.logout();
		return new NamespaceListing(b.build());
	}

}
