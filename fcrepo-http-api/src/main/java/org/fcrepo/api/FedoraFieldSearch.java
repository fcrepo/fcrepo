
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

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
import javax.ws.rs.core.UriInfo;

import org.fcrepo.AbstractResource;
import org.fcrepo.RdfLexicon;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * @author Frank Asseg
 */

@Component
@Path("/rest/fcr:search")
public class FedoraFieldSearch extends AbstractResource implements
        FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(FedoraFieldSearch.class);

    @GET
    @Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES, TEXT_HTML})
    public Dataset searchSubmitRdf(@QueryParam("q")
    final String terms, @QueryParam("offset")
    @DefaultValue("0")
    final long offset, @QueryParam("limit")
    @DefaultValue("25")
    final int limit, @Context
    final Request request, @Context
    final UriInfo uriInfo) throws RepositoryException {


        if (terms.isEmpty()) {
            LOGGER.trace("Received search request, but terms was empty. Aborting.");
            throw new WebApplicationException(Response.status(
                    Response.Status.BAD_REQUEST).entity(
                    "q parameter is mandatory").build());
        }

        final Session session = getAuthenticatedSession();
        try {
            LOGGER.debug("Received search request with search terms {}, offset {}, and limit {}", terms, offset, limit);

            final Resource searchResult =
                    ResourceFactory.createResource(uriInfo.getRequestUri()
                            .toASCIIString());

            final Dataset dataset =
                    nodeService.searchRepository(new HttpGraphSubjects(
                            FedoraNodes.class, uriInfo), searchResult, session,
                            terms, limit, offset);

            final Model searchModel = ModelFactory.createDefaultModel();
            Map<String, ?> pathMap = ImmutableBiMap.of("q", terms, "offset", offset + limit, "limit", limit);
            searchModel.add(searchResult, RdfLexicon.SEARCH_NEXT_PAGE, searchModel.createResource(uriInfo.getRequestUriBuilder().path(FedoraFieldSearch.class).buildFromMap(pathMap).toString()));
            dataset.addNamedModel("search-pagination", searchModel);

            return dataset;

        } finally {
            session.logout();
        }
    }

}
