
package org.fcrepo.api;

import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import org.fcrepo.AbstractResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.RDFMediaType;
import org.fcrepo.provider.GraphStreamingOutput;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Frank Asseg
 */

@Component
@Path("/rest/fcr:search")
public class FedoraFieldSearch extends AbstractResource implements
        FedoraJcrTypes {

    private static final Logger logger = getLogger(FedoraFieldSearch.class);


    @GET
    @Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public GraphStreamingOutput searchSubmitRdf(@QueryParam("q") final String terms,
            @QueryParam("offset") @DefaultValue("0") final long offset,
            @QueryParam("limit") @DefaultValue("25") final int limit, @Context final Request request)
                    throws RepositoryException{

        if (terms.isEmpty()) {
            throw new WebApplicationException(
                                                     Response.status(Response.Status.BAD_REQUEST)
                                                             .entity("q parameter is mandatory")
                                                             .build()
            );
        }

        final Session session = getAuthenticatedSession();
        try{
            /* select the best response type */
            final Variant bestPossibleResponse = request.selectVariant(RDFMediaType.POSSIBLE_RDF_VARIANTS);


            final Resource searchResult = ResourceFactory.createResource(uriInfo.getRequestUri().toASCIIString());
            final GraphStore graphStore = nodeService.searchRepository(new HttpGraphSubjects(FedoraNodes.class, uriInfo), searchResult, session, terms, limit, offset);

            return new GraphStreamingOutput(graphStore, bestPossibleResponse.getMediaType());

        } finally{
            session.logout();
        }
    }

}
