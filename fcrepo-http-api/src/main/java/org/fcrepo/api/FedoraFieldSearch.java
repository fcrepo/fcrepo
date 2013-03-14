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

	@GET
    @Produces(TEXT_HTML)
    public Response searchForm() throws LoginException,
            RepositoryException {
   	
    	VelocityViewer view = new VelocityViewer();    	
		return ok(view.getViewer("search-results-form.vm", null, null)).build();
    }
    
    @POST
    @Produces(TEXT_HTML)
    public Response searchSubmit(@FormParam("terms") String terms, @FormParam("maxResults") String maxResults) throws LoginException,
            RepositoryException {
		
    	logger.debug("Searching for " + terms);
		VelocityViewer view = new VelocityViewer();
		FieldSearchResult fsr = search(terms, Integer.parseInt(maxResults));
		//TODO - host and port are hardcoded in form
		return ok(view.getViewer("search-results-form.vm", "results", fsr)).build();
    }
    
    public FieldSearchResult search(String terms, int maxResults) throws LoginException,
    		RepositoryException {
    	final Session session = repo.login();

    	List<ObjectFields> fieldObjects = new ArrayList<ObjectFields>();
		QueryManager queryManager = session.getWorkspace().getQueryManager();

		String language = Query.JCR_SQL2;
		//TODO expand query to other fields
		String expression = "SELECT * FROM [fedora:object] WHERE [dc:identifier] = '" + terms + "'";
		Query query = queryManager.createQuery(expression,language);

		QueryResult queryResults = query.execute();
		RowIterator rowIter = queryResults.getRows();
		int size = (int)rowIter.getSize();
		logger.debug(size + " results found");

		NodeIterator nodeIter = queryResults.getNodes();		
		
		while ( nodeIter.hasNext() ) {
			ObjectFields obj = new ObjectFields();
			try {
			    Node node = nodeIter.nextNode();
			    obj.setPid(node.getName());
			    obj.setPath(node.getPath());
			    fieldObjects.add(obj);
			} catch (RepositoryException ex) {
				logger.error(ex.getMessage());
			}
		}
		
		FieldSearchResult fsr = new FieldSearchResult(fieldObjects, 0, maxResults, size);

		session.logout();
		
		return fsr;
    }
}
