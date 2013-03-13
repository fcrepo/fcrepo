
package org.fcrepo.api;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.ImmutableMap.builder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.services.PathService.OBJECT_PATH;
import static org.fcrepo.utils.FedoraJcrTypes.DC_IDENTIFER;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.value2string;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
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

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.access.DescribeRepository;
import org.fcrepo.provider.VelocityViewer;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap.Builder;

/**
 * 
 * @author cabeer
 * @author ajs6f
 */

@Path("")
public class FedoraRepository extends AbstractResource {

    private static final Logger logger = LoggerFactory
            .getLogger(FedoraRepository.class);
    
    @GET
    @Path("/search")
    @Produces(TEXT_HTML)
    public Response searchForm() throws LoginException,
            RepositoryException {
   	
    	VelocityViewer view = new VelocityViewer();    	
		return ok(view.getViewer("search-results-form.vm", null, null)).build();
    }
    
    @POST
    @Path("/search")
    @Produces(TEXT_HTML)
    public Response searchSubmit(@FormParam("terms") String terms, @FormParam("maxResults") String maxResults) throws LoginException,
            RepositoryException {
		
    	logger.debug("Searching for " + terms);
		VelocityViewer view = new VelocityViewer();
    	
		return ok(view.getViewer("search-results-form.vm", "results", search(terms))).build();
    }
    
    public Map<String, String> search(String terms) throws LoginException,
    		RepositoryException{
    	final Session session = repo.login();

    	//TODO temp object
    	Map<String, String> fieldResults = new HashMap<String, String>();
		QueryManager queryManager = session.getWorkspace().getQueryManager();

		String language = Query.JCR_SQL2;
		//TODO expand query to other fields
		String expression = "SELECT * FROM [fedora:object] WHERE [dc:identifier] = '" + terms + "'";
		Query query = queryManager.createQuery(expression,language);

		QueryResult result = query.execute();
		RowIterator rowIter = result.getRows();
		logger.debug(rowIter.getSize() + " results found");

		NodeIterator nodeIter = result.getNodes();		
		
		while ( nodeIter.hasNext() ) {
			try {
			    Node node = nodeIter.nextNode();
			    fieldResults.put(node.getName(), node.getPath());
			} catch (RepositoryException ex) {
				logger.debug("Couldn't add to fieldResults");
				logger.error(ex.getMessage());
			}
		}

		session.logout();
		return fieldResults;
    }

    @GET
    @Path("/describe/modeshape")
    public Response describeModeshape() throws JsonGenerationException,
            JsonMappingException, IOException, RepositoryException {
        final Session session = repo.login();
        logger.debug("Repository name: " +
                repo.getDescriptor(Repository.REP_NAME_DESC));
        final Builder<String, Object> repoproperties = builder();
        for (final String key : repo.getDescriptorKeys()) {
            if (repo.getDescriptor(key) != null)
                repoproperties.put(key, repo.getDescriptor(key));
        }

        // add in node namespaces
        final NamespaceRegistry reg =
                session.getWorkspace().getNamespaceRegistry();
        final Builder<String, String> namespaces = builder();
        for (final String prefix : reg.getPrefixes()) {
            namespaces.put(prefix, reg.getURI(prefix));
        }
        repoproperties.put("node.namespaces", namespaces.build());

        // add in node types
        final NodeTypeManager ntmanager =
                (NodeTypeManager) session.getWorkspace().getNodeTypeManager();
        final Builder<String, String> nodetypes = builder();
        NodeTypeIterator i = ntmanager.getAllNodeTypes();
        while (i.hasNext()) {
            NodeType nt = i.nextNodeType();
            nodetypes.put(nt.getName(), nt.toString());
        }
        repoproperties.put("node.types", nodetypes.build());
        session.logout();
        return ok(repoproperties.build().toString()).build();
    }
    
    @GET
    @Path("/describe")
    @Produces(TEXT_HTML)
    public String describeHtml() throws LoginException,
            RepositoryException {

        Session session = repo.login();
        DescribeRepository description = new DescribeRepository();
        description.repositoryBaseURL = uriInfo.getBaseUri();
        description.sampleOAIURL =
                uriInfo.getBaseUriBuilder().path(OBJECT_PATH + "/123/oai_dc")
                        .build();
        description.repositorySize = getRepositorySize(session);
        description.numberOfObjects =
                session.getNode("/objects").getNodes().getSize();
        session.logout();
        VelocityViewer view = new VelocityViewer();
        return view.getRepoInfo(description);
    }

    @GET
    @Path("/describe")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public DescribeRepository describe() throws LoginException,
            RepositoryException {

        Session session = repo.login();
        DescribeRepository description = new DescribeRepository();
        description.repositoryBaseURL = uriInfo.getBaseUri();
        description.sampleOAIURL =
                uriInfo.getBaseUriBuilder().path(OBJECT_PATH + "/123/oai_dc")
                        .build();
        description.repositorySize = getRepositorySize(session);
        description.numberOfObjects =
                session.getNode("/objects").getNodes().getSize();
        session.logout();
        return description;
    }

}
