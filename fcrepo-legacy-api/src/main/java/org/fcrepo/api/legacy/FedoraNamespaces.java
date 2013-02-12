
package org.fcrepo.api.legacy;

import static com.google.common.collect.ImmutableSet.builder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;

import java.io.IOException;
import java.net.URI;

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

import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.NamespaceListing;
import org.fcrepo.jaxb.responses.NamespaceListing.Namespace;

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

    /**
     * Creates a new namespace in the JCR for use in identifing objects.
     * 
     * @param prefix Prefix to use
     * @param uri Uri to use
     * @return 201
     * @throws RepositoryException
     */
    @POST
    @Path("/{prefix}")
    public Response registerObjectNamespace(@PathParam("prefix")
    final String prefix, final String uri) throws RepositoryException {

        final Session session = repo.login();
        final NamespaceRegistry r =
                session.getWorkspace().getNamespaceRegistry();
        r.registerNamespace(prefix, uri);
        session.logout();
        return created(uriInfo.getAbsolutePath()).build();
    }

    /**
     * Register multiple object namespaces.
     * 
     * @param nses A set of namespaces in JAXB-specified format.
     * @return 201
     * @throws RepositoryException
     */
    @POST
    @Consumes({TEXT_XML, APPLICATION_JSON})
    public Response registerObjectNamespaces(final NamespaceListing nses)
            throws RepositoryException {

        final Session session = repo.login();
        final NamespaceRegistry r =
                session.getWorkspace().getNamespaceRegistry();
        for (Namespace ns : nses.namespaces)
            r.registerNamespace(ns.prefix, ns.uri.toString());
        session.logout();
        return created(uriInfo.getAbsolutePath()).build();
    }

    /**
     * Retrieve a namespace URI from a prefix.
     * 
     * @param prefix The prefix to search.
     * @return A JAXB-specified format Namespace.
     * @throws RepositoryException
     */
    @GET
    @Path("/{prefix}")
    @Produces(APPLICATION_JSON)
    public Response retrieveObjectNamespace(@PathParam("ns")
    final String prefix) throws RepositoryException {

        final Session session = repo.login();
        final NamespaceRegistry r =
                session.getWorkspace().getNamespaceRegistry();

        if (ImmutableSet.copyOf(r.getPrefixes()).contains(prefix)) {
            final Namespace ns =
                    new Namespace(prefix, URI.create(r.getURI(prefix)));
            session.logout();
            return ok(ns).build();
        } else {
            session.logout();
            return four04;
        }
    }

    /**
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    @GET
    @Produces({TEXT_XML, APPLICATION_JSON})
    public NamespaceListing getNamespaces() throws RepositoryException,
            IOException {

        final Session session = repo.login();
        final NamespaceRegistry r =
                session.getWorkspace().getNamespaceRegistry();
        final Builder<Namespace> b = builder();
        for (final String prefix : r.getPrefixes()) {
            b.add(new Namespace(prefix, URI.create(r.getURI(prefix))));
        }
        session.logout();
        return new NamespaceListing(b.build());
    }

}
