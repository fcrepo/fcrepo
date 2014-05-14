/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.nil;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.RdfLexicon.FIRST_PAGE;
import static org.fcrepo.kernel.RdfLexicon.NEXT_PAGE;
import static org.fcrepo.kernel.RdfLexicon.PAGE;
import static org.fcrepo.kernel.RdfLexicon.PAGE_OF;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_HAS_MORE;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_ITEMS_PER_PAGE;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_OFFSET;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_PAGE;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_TERMS;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Basic administrative search across the repository
 *
 * @author Frank Asseg
 * @author ajs6f
 */

@Component
@Scope("prototype")
@Path("/fcr:search")
public class FedoraFieldSearch extends AbstractResource implements
        FedoraJcrTypes {

    public static final String OFFSET_PARAM = "offset";
    public static final String QUERY_PARAM = "q";
    public static final String LIMIT_PARAM = "limit";
    @InjectedSession
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraFieldSearch.class);

    /**
     * A stub method so we can return a text/html representation using
     * the right template.
     *
     * @param terms
     * @param offset
     * @param limit
     * @param uriInfo
     * @return search results in HTML format
     * @throws RepositoryException
     */
    @GET
    @Timed
    @HtmlTemplate("search:results")
    @Produces({TEXT_HTML})
    public Dataset searchSubmitHtml(@QueryParam(QUERY_PARAM)
                                        final String terms,
                                    @QueryParam(OFFSET_PARAM) @DefaultValue("0")
                                    final long offset,
                                    @QueryParam(LIMIT_PARAM)
                                    @DefaultValue("25")
                                    final int limit,
                                    @Context final HttpServletResponse servletResponse,
                                    @Context
                                    final UriInfo uriInfo) throws RepositoryException {
        return getSearchDataset(terms, offset, limit, servletResponse, uriInfo);
    }

    /**
     * Execute a basic full-text search across the repository
     *
     * GET /fcr:search?q=term
     *
     * @param terms
     * @param offset
     * @param limit
     * @param request
     * @param uriInfo
     * @return full text search results in the given format
     * @throws RepositoryException
     */
    @GET
    @Timed
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, TEXT_PLAIN, TURTLE_X})
    public Dataset searchSubmitRdf(@QueryParam(QUERY_PARAM) final String terms,
            @QueryParam(OFFSET_PARAM) @DefaultValue("0") final long offset,
            @QueryParam(LIMIT_PARAM) @DefaultValue("25") final int limit,
            @Context final Request request,
            @Context final HttpServletResponse servletResponse,
            @Context final UriInfo uriInfo) throws RepositoryException {

        if (terms == null) {
            LOGGER.trace("Received search request, but terms were empty. Aborting.");
            throw new WebApplicationException(status(BAD_REQUEST).entity(
                    "q parameter is mandatory").build());
        }

        return getSearchDataset(terms, offset, limit, servletResponse, uriInfo);
    }

    private Dataset getSearchDataset(final String terms,
                                     final long offset,
                                     final int limit,
                                     final HttpServletResponse servletResponse,
                                     final UriInfo uriInfo)
        throws RepositoryException {

        try {
            LOGGER.debug(
                    "Received search request with search terms {}, offset {}, and limit {}",
                    terms, offset, limit);

            final Resource searchResult;

            if (terms == null) {
                searchResult = createResource(uriInfo.getBaseUriBuilder()
                                                  .path(FedoraFieldSearch.class)
                                                  .build().toString());
            } else {
                searchResult = createResource(uriInfo.getBaseUriBuilder()
                                                  .path(FedoraFieldSearch.class)
                                                  .queryParam(QUERY_PARAM, terms)
                                                  .build().toString());
            }

            final HttpIdentifierTranslator subjects = new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

            final Dataset dataset =
                    repositoryService.searchRepository(subjects, searchResult,
                            session, terms, limit, offset);

            final Model searchModel = createDefaultModel();
            if (terms != null) {
                final Resource pageResource = createResource(uriInfo.getRequestUri().toASCIIString());
                searchModel.add(pageResource, type, SEARCH_PAGE);
                searchModel.add(pageResource, type, PAGE);
                searchModel.add(pageResource, PAGE_OF, searchResult);


                searchModel.add(pageResource,
                                   SEARCH_ITEMS_PER_PAGE,
                                   searchModel.createTypedLiteral(limit));
                searchModel.add(pageResource,
                                   SEARCH_OFFSET,
                                   searchModel.createTypedLiteral(offset));
                searchModel.add(pageResource, SEARCH_TERMS, terms);

                if (dataset.getDefaultModel()
                        .contains(searchResult,
                                     SEARCH_HAS_MORE,
                                     searchModel.createTypedLiteral(true))) {

                    final Resource nextPageResource =
                        searchModel.createResource(uriInfo
                                                       .getBaseUriBuilder()
                                                       .path(FedoraFieldSearch.class)
                                                       .queryParam(QUERY_PARAM, terms)
                                                       .queryParam(OFFSET_PARAM, offset + limit)
                                                       .queryParam(LIMIT_PARAM, limit)
                                                       .build()
                                                       .toString());
                    searchModel.add(pageResource, NEXT_PAGE, nextPageResource);
                } else {
                    searchModel.add(pageResource, NEXT_PAGE, nil);
                }

                final String firstPage = uriInfo
                                       .getBaseUriBuilder()
                                       .path(FedoraFieldSearch.class)
                                       .queryParam(QUERY_PARAM, terms)
                                       .queryParam(OFFSET_PARAM, 0)
                                       .queryParam(LIMIT_PARAM, limit)
                                       .build()
                                       .toString();
                final Resource firstPageResource =
                    searchModel.createResource(firstPage);
                searchModel.add(subjects.getContext(), FIRST_PAGE, firstPageResource);

                servletResponse.addHeader("Link", "<" + firstPage + ">;rel=\"first\"");

                dataset.addNamedModel("search-pagination", searchModel);
            }

            return dataset;

        } finally {
            session.logout();
        }
    }
}
