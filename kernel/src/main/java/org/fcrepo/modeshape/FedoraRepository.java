package org.fcrepo.modeshape;

import static com.google.common.collect.ImmutableMap.builder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;

import java.io.IOException;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.fcrepo.modeshape.jaxb.responses.DescribeRepository;
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
	@Path("/describe/modeshape")
	public Response describeModeshape() throws JsonGenerationException,
			JsonMappingException, IOException, RepositoryException {
		final Session session = repo.login();
		logger.debug("Repository name: "
				+ repo.getDescriptor(Repository.REP_NAME_DESC));
		final Builder<String, Object> repoproperties = builder();
		for (final String key : repo.getDescriptorKeys()) {
			if (repo.getDescriptor(key) != null)
				repoproperties.put(key, repo.getDescriptor(key));
		}

		// add in node namespaces
		final NamespaceRegistry reg = session.getWorkspace()
				.getNamespaceRegistry();
		final Builder<String, String> namespaces = builder();
		for (final String prefix : reg.getPrefixes()) {
			namespaces.put(prefix, reg.getURI(prefix));
		}
		repoproperties.put("node.namespaces", namespaces.build());

		// add in node types
		final NodeTypeManager ntmanager = (NodeTypeManager) session
				.getWorkspace().getNodeTypeManager();
		final Builder<String, String> nodetypes = builder();
		NodeTypeIterator i = ntmanager.getAllNodeTypes();
		while (i.hasNext()) {
			NodeType nt = i.nextNodeType();
			nodetypes.put(nt.getName(), nt.toString());
		}
		repoproperties.put("node.types", nodetypes.build());
		Map<String, Object> props = repoproperties.build();
		session.logout();
		return ok(mapper.writerWithType(Map.class).writeValueAsString(props))
				.build();
	}

	@GET
	@Path("/describe")
	@Produces({ TEXT_XML, APPLICATION_JSON })
	public DescribeRepository describe() throws LoginException,
			RepositoryException {

		Session session = repo.login();
		DescribeRepository description = new DescribeRepository();
		description.repositorySize = getRepositorySize(session);
		description.numberOfObjects = session.getNode("/objects").getNodes()
				.getSize();
		session.logout();
		return description;
	}

}
