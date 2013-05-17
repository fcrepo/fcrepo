
package org.fcrepo.api;

import static com.google.common.collect.ImmutableList.builder;
import static java.lang.Integer.parseInt;
import static javax.jcr.query.Query.JCR_SQL2;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.http.RDFMediaType;
import org.fcrepo.jaxb.search.FieldSearchResult;
import org.fcrepo.jaxb.search.ObjectFields;
import org.fcrepo.provider.ModelStreamingOutput;
import org.fcrepo.provider.VelocityViewer;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.JcrRdfTools;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Vincent Nguyen
 */

@Component
@Path("/rest/fcr:search")
public class FedoraFieldSearch extends AbstractResource implements
        FedoraJcrTypes {

    private static final Logger logger = getLogger(FedoraFieldSearch.class);

    private static final String QUERY_STRING = buildQueryString();

    @GET
	@Timed
    @Produces(TEXT_HTML)
    public String searchForm() throws RepositoryException {
        return new VelocityViewer().getFieldSearch(null);
    }

    @POST
    @Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public Response searchSubmitRdf(@FormParam("terms") final String terms,
            @FormParam("offSet") @DefaultValue("0") final String offSet,
            @FormParam("maxResults") final String maxResults, @Context final Request request)
                    throws RepositoryException{

        final Session session = getAuthenticatedSession();
        try{
            /* select the best response type */
            final Variant bestPossibleResponse = request.selectVariant(RDFMediaType.POSSIBLE_RDF_VARIANTS);

            /* construct the query from the user's search terms */
            final Query query = getQuery(session.getWorkspace().getQueryManager(), session.getValueFactory(), terms);

            /* perform the actual search in the repository */
            final FieldSearchResult result = search(query, parseInt(offSet), parseInt(maxResults));
            result.setSearchTerms(terms);

            final Model model = ModelFactory.createDefaultModel();
            final Resource searchResult = model.createResource(uriInfo.getAbsolutePath().toASCIIString());

            /* add the result description to the RDF model */
            searchResult.addProperty(model.createProperty("info:fedora/fedora-system:def/search#numSearchResults"),
                    model.createTypedLiteral(result.getSize()));
            searchResult.addProperty(model.createProperty("info:fedora/fedora-system:def/search#searchTerms"),
                    result.getSearchTerms());
            searchResult.addProperty(model.createProperty("info:fedora/fedora-system:def/search#maxNumResults"),
                    model.createTypedLiteral(result.getMaxResults()));

            /* and add the RDF model of each found fedora:resource node to the response model */
            for (ObjectFields field : result.getObjectFieldsList()){
                final FedoraResource fo = this.nodeService.getObject(session, field.getPath());
                Resource objResource = model.createResource(uriInfo.getBaseUri().toASCIIString() + "rest" + field.getPath());
                model.add(JcrRdfTools.getJcrPropertiesModel(FedoraResource.DEFAULT_SUBJECT_FACTORY, fo.getNode()));
            }

            return Response.ok(new ModelStreamingOutput(model, bestPossibleResponse.getMediaType())).build();
        }finally{
            session.logout();
        }
    }

    @POST
	@Timed
    @Produces(TEXT_HTML)
    public String searchSubmit(@FormParam("terms")
    final String terms, @FormParam("offSet")
    @DefaultValue("0")
    final String offSet, @FormParam("maxResults")
    final String maxResults) throws RepositoryException {

        final Session session = getAuthenticatedSession();
        final QueryManager queryManager =
                session.getWorkspace().getQueryManager();
        final ValueFactory valueFactory = session.getValueFactory();
        final VelocityViewer view = new VelocityViewer();

        logger.debug("Searching for " + terms);

        final Query query = getQuery(queryManager, valueFactory, terms);
        logger.debug("statement is " + query.getStatement());

        final FieldSearchResult fsr =
                search(query, parseInt(offSet), parseInt(maxResults));
        fsr.setSearchTerms(terms);

        session.logout();

        return view.getFieldSearch(fsr);
    }

    Query getQuery(final QueryManager queryManager,
            final ValueFactory valueFactory, final String terms)
            throws RepositoryException {
        final Query query = queryManager.createQuery(QUERY_STRING, JCR_SQL2);
        query.bindValue("sterm", valueFactory.createValue("%" + terms + "%"));
        logger.debug("statement is " + query.getStatement());
        return query;
    }

    /**
     * Searches the repository using JCR SQL2 queries and returns a FieldSearchResult object
     * @param sqlExpression
     * @param offSet
     * @param maxResults
     * @return
     * @throws LoginException
     * @throws RepositoryException
     */
    public FieldSearchResult search(final Query query, final int offSet,
            final int maxResults) throws RepositoryException {

        final ImmutableList.Builder<ObjectFields> fieldObjects = builder();

        final QueryResult queryResults = query.execute();

        final NodeIterator nodeIter = queryResults.getNodes();
        final int size = (int) nodeIter.getSize();
        logger.debug(size + " results found");

        //add the next set of results to the fieldObjects starting at offSet for pagination
        int i = offSet;
        nodeIter.skip(offSet);
        while (nodeIter.hasNext() && i < offSet + maxResults) {
            final ObjectFields obj = new ObjectFields();
            try {
                final Node node = nodeIter.nextNode();
                obj.setPid(node.getName());
                obj.setPath(node.getPath());
                fieldObjects.add(obj);
            } catch (final RepositoryException ex) {
                logger.error(ex.getMessage());
            }
            i++;
        }

        final FieldSearchResult fsr =
                new FieldSearchResult(fieldObjects.build(), offSet, maxResults,
                        size);
        fsr.setStart(offSet);
        fsr.setMaxResults(maxResults);

        return fsr;
    }

    public static String buildQueryString() {
        //TODO expand to more fields
        final String sqlExpression =
                "SELECT * FROM [" + FEDORA_RESOURCE + "] WHERE [dc:identifier] like $sterm OR [dc:title] like $sterm";
        return sqlExpression;
    }
}
