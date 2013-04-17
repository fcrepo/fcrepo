
package org.fcrepo.api;

import static com.google.common.collect.ImmutableList.builder;
import static java.lang.Integer.parseInt;
import static javax.jcr.query.Query.JCR_SQL2;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.search.FieldSearchResult;
import org.fcrepo.jaxb.search.ObjectFields;
import org.fcrepo.provider.VelocityViewer;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

/**
 * @author Vincent Nguyen
 */

@Component
@Path("/rest/search")
public class FedoraFieldSearch extends AbstractResource implements
        FedoraJcrTypes {

    private static final Logger logger = getLogger(FedoraFieldSearch.class);

    private static final String QUERY_STRING = buildQueryString();

    @GET
    @Produces(TEXT_HTML)
    public String searchForm() throws LoginException, RepositoryException {
        return new VelocityViewer().getFieldSearch(null);
    }

    @POST
    @Produces(TEXT_HTML)
    public String searchSubmit(@FormParam("terms")
    String terms, @FormParam("offSet")
    String offSet, @FormParam("maxResults")
    String maxResults) throws LoginException, RepositoryException {

        final Session session = getAuthenticatedSession();
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        ValueFactory valueFactory = session.getValueFactory();
        VelocityViewer view = new VelocityViewer();

        logger.debug("Searching for " + terms);

        Query query = getQuery(queryManager, valueFactory, terms);
        logger.debug("statement is " + query.getStatement());

        if (offSet == null) {
            offSet = "0";
        }

        FieldSearchResult fsr =
                search(query, parseInt(offSet), parseInt(maxResults));
        fsr.setSearchTerms(terms);

        session.logout();

        return view.getFieldSearch(fsr);
    }

    Query getQuery(QueryManager queryManager, ValueFactory valueFactory,
            String terms) throws InvalidQueryException, RepositoryException {
        Query query = queryManager.createQuery(QUERY_STRING, JCR_SQL2);
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
    public FieldSearchResult search(Query query, int offSet, int maxResults)
            throws LoginException, RepositoryException {

        ImmutableList.Builder<ObjectFields> fieldObjects = builder();

        QueryResult queryResults = query.execute();

        NodeIterator nodeIter = queryResults.getNodes();
        int size = (int) nodeIter.getSize();
        logger.debug(size + " results found");

        //add the next set of results to the fieldObjects starting at offSet for pagination
        int i = offSet;
        nodeIter.skip(offSet);
        while (nodeIter.hasNext() && i < offSet + maxResults) {
            ObjectFields obj = new ObjectFields();
            try {
                Node node = nodeIter.nextNode();
                obj.setPid(node.getName());
                obj.setPath(node.getPath());
                fieldObjects.add(obj);
            } catch (RepositoryException ex) {
                logger.error(ex.getMessage());
            }
            i++;
        }

        FieldSearchResult fsr =
                new FieldSearchResult(fieldObjects.build(), offSet, maxResults,
                        size);
        fsr.setStart(offSet);
        fsr.setMaxResults(maxResults);

        return fsr;
    }

    public static String buildQueryString() {
        //TODO expand to more fields
        String sqlExpression =
                "SELECT * FROM [" + FEDORA_OBJECT + "] WHERE [" +
                        DC_IDENTIFIER + "] like $sterm OR [" + DC_TITLE +
                        "] like $sterm";
        return sqlExpression;
    }
}
