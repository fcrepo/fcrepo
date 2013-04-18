
package org.fcrepo.api.legacy;

import static com.google.common.collect.ImmutableMap.builder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.services.PathService.OBJECT_PATH;

import java.io.IOException;

import javax.inject.Inject;
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
import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.access.DescribeRepository;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap.Builder;

/**
 * 
 * @author cbeer
 * @author ajs6f
 */

@Path("/v3")
@Component("fedoraLegacyRepository")
public class FedoraRepository extends AbstractResource {

    private static final Logger logger = LoggerFactory
            .getLogger(FedoraRepository.class);
    
    @Autowired
    Repository repo;

    @GET
    @Path("/describe/modeshape")
    public Response describeModeshape() throws JsonGenerationException,
            JsonMappingException, IOException, RepositoryException {
        final Session session = getAuthenticatedSession();
        logger.debug("Repository name: " +
                repo.getDescriptor(Repository.REP_NAME_DESC));
        final Builder<String, Object> repoproperties = builder();
        for (final String key : repo.getDescriptorKeys()) {
            if (repo.getDescriptor(key) != null) {
                repoproperties.put(key, repo.getDescriptor(key));
            }
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
    @Produces({TEXT_XML, APPLICATION_JSON})
    public DescribeRepository describe() throws LoginException,
            RepositoryException {

        final Session session = getAuthenticatedSession();
        DescribeRepository description = new DescribeRepository();
        description.repositoryBaseURL = uriInfo.getBaseUri();
        description.sampleOAIURL =
                uriInfo.getBaseUriBuilder().path(OBJECT_PATH + "/123/oai_dc")
                        .build();
        description.repositorySize = objectService.getAllObjectsDatastreamSize();
        description.numberOfObjects =
                session.getNode("/objects").getNodes().getSize();
        session.logout();
        return description;
    }

}
