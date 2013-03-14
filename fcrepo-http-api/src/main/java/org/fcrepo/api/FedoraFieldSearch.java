package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.ok;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.search.FieldSearchResult;
import org.fcrepo.jaxb.search.ObjectFields;
import org.fcrepo.provider.VelocityViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vincent Nguyen
 */

@Path("/search")
public class FedoraFieldSearch extends AbstractResource {
	private static final Logger logger = LoggerFactory
            .getLogger(FedoraFieldSearch.class);

	private QueryResult queryResults = null;
	
	@GET
    @Produces(TEXT_HTML)
    public Response searchForm() throws LoginException,
            RepositoryException {
   	
    	VelocityViewer view = new VelocityViewer();    	
		return ok(view.getViewer("search-results-form.vm", null, null)).build();
    }
    
    @POST
    @Produces(TEXT_HTML)
    public Response searchSubmit(@FormParam("terms") String terms, 
    		@FormParam("offSet") String offSet, 
    		@FormParam("maxResults") String maxResults) throws LoginException,
            RepositoryException {
    	
    	final Session session = repo.login();
    	QueryManager queryManager = session.getWorkspace().getQueryManager();
		VelocityViewer view = new VelocityViewer();
		
		logger.debug("Searching for " + terms);
		
		//TODO expand to more fields
		String sqlExpression = "SELECT * FROM [fedora:object] WHERE";
		sqlExpression += " [dc:identifier] like $sterm";
		sqlExpression += " OR [dc:title] like $sterm";
		String language = Query.JCR_SQL2;
		Query query = queryManager.createQuery(sqlExpression,language);
		query.bindValue("sterm", session.getValueFactory().createValue("%" + terms + "%"));
		logger.debug("statement is " + query.getStatement());
		
		if (offSet == null) {
			offSet = "0";
		}
		
		FieldSearchResult fsr = search(query, Integer.parseInt(offSet), Integer.parseInt(maxResults));
		fsr.setSearchTerms(terms);
		
		session.logout();
		
		return ok(view.getViewer("search-results-form.vm", "results", fsr)).build();
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
    public FieldSearchResult search(Query query, int offSet, int maxResults) throws LoginException,
    		RepositoryException {
    	
    	List<ObjectFields> fieldObjects = new ArrayList<ObjectFields>();

		//if offSet is 0, we assume it's the first query and set the queryResults
		if (offSet == 0) {
			queryResults = query.execute();
		}

		NodeIterator nodeIter = queryResults.getNodes();	
		int size = (int)nodeIter.getSize();
		logger.debug(size + " results found");
		
		//add the next set of results to the fieldObjects starting at offSet for pagination
		int i=offSet;
		nodeIter.skip((long)offSet);
		while ( nodeIter.hasNext() && i < (offSet + maxResults)) {
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
		
		FieldSearchResult fsr = new FieldSearchResult(fieldObjects, offSet, maxResults, size);
		fsr.setStart(offSet);
		fsr.setMaxResults(maxResults);

		return fsr;
    }
}
