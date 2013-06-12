
package org.fcrepo.api.repository;

import static com.hp.hpl.jena.update.UpdateAction.parseExecute;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.status;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.fcrepo.AbstractResource;
import org.fcrepo.responses.HtmlTemplate;
import org.fcrepo.session.InjectedSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;

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
@Component
@Scope("prototype")
@Path("/fcr:namespaces")
public class FedoraRepositoryNamespaces extends AbstractResource {

    @InjectedSession
    protected Session session;

    /**
     * Register multiple object namespaces.
     *
     * @return 204
     * @throws RepositoryException
     */
    @POST
    @Timed
    @Consumes({contentTypeSPARQLUpdate})
    public Response updateNamespaces(final InputStream requestBodyStream)
            throws RepositoryException, IOException {

        try {
            final Dataset dataset =
                    nodeService.getNamespaceRegistryGraph(session);
            parseExecute(IOUtils.toString(requestBodyStream), dataset);
            session.save();
            return status(SC_NO_CONTENT).build();
        } finally {
            session.logout();
        }
    }

    /**
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    @GET
    @Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES,
            TEXT_HTML})
    @HtmlTemplate("jcr:namespaces")
    public Dataset getNamespaces() throws RepositoryException, IOException {

        try {
            final Dataset dataset =
                    nodeService.getNamespaceRegistryGraph(session);
            return dataset;
        } finally {
            session.logout();
        }
    }

    public void setSession(final Session session) {
        this.session = session;
    }
}
