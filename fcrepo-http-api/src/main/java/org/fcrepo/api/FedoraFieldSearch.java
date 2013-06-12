
package org.fcrepo.api;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.RdfLexicon.SEARCH_HAS_MORE;
import static org.fcrepo.RdfLexicon.SEARCH_NEXT_PAGE;
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
import javax.ws.rs.core.UriInfo;

import org.fcrepo.AbstractResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.responses.HtmlTemplate;
import org.fcrepo.session.InjectedSession;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Frank Asseg
 * @author ajs6f
 */

@Component
@Scope("prototype")
@Path("/fcr:search")
public class FedoraFieldSearch extends AbstractResource implements
        FedoraJcrTypes {

    @InjectedSession
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraFieldSearch.class);

    @GET
    @Timed
    @HtmlTemplate("search:results")
    @Produces({TEXT_HTML})
    public Dataset searchSubmitHtml(@QueryParam("q")
    final String terms, @QueryParam("offset")
    @DefaultValue("0")
    final long offset, @QueryParam("limit")
    @DefaultValue("25")
    final int limit, @Context
    final Request request, @Context
    final UriInfo uriInfo) throws RepositoryException {

        return getSearchDataset(terms, offset, limit, uriInfo);
    }

    @GET
    @Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public Dataset searchSubmitRdf(@QueryParam("q")
    final String terms, @QueryParam("offset")
    @DefaultValue("0")
    final long offset, @QueryParam("limit")
    @DefaultValue("25")
    final int limit, @Context
    final Request request, @Context
    final UriInfo uriInfo) throws RepositoryException {

        if (terms == null) {
            LOGGER.trace("Received search request, but terms was empty. Aborting.");
            throw new WebApplicationException(status(BAD_REQUEST).entity(
                    "q parameter is mandatory").build());
        }

        return getSearchDataset(terms, offset, limit, uriInfo);
    }

    private Dataset getSearchDataset(final String terms, final long offset,
            final int limit, final UriInfo uriInfo) throws RepositoryException {

        try {
            LOGGER.debug(
                    "Received search request with search terms {}, offset {}, and limit {}",
                    terms, offset, limit);

            final Resource searchResult =
                    createResource(uriInfo.getRequestUri().toASCIIString());

            final Dataset dataset =
                    nodeService.searchRepository(new HttpGraphSubjects(
                            FedoraNodes.class, uriInfo), searchResult, session,
                            terms, limit, offset);

            final Model searchModel = createDefaultModel();
            if (terms != null &&
                    dataset.getDefaultModel().contains(searchResult,
                            SEARCH_HAS_MORE,
                            searchModel.createTypedLiteral(true))) {
                final Map<String, ?> pathMap =
                        ImmutableBiMap.of("q", terms, "offset", offset + limit,
                                "limit", limit);
                searchModel.add(searchResult, SEARCH_NEXT_PAGE, searchModel
                        .createResource(uriInfo.getBaseUriBuilder().path(
                                FedoraFieldSearch.class).buildFromMap(pathMap)
                                .toString()));
                dataset.addNamedModel("search-pagination", searchModel);
            }

            return dataset;

        } finally {
            session.logout();
        }
    }

    public void setSession(final Session session) {
        this.session = session;
    }

}
