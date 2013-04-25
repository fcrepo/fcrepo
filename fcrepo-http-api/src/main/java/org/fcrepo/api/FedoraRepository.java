
package org.fcrepo.api;

import static com.google.common.collect.ImmutableMap.builder;
import static javax.jcr.Repository.REP_NAME_DESC;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.services.PathService.OBJECT_PATH;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.yammer.metrics.annotation.Timed;
import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.access.DescribeCluster;
import org.fcrepo.jaxb.responses.access.DescribeRepository;
import org.fcrepo.provider.VelocityViewer;
import org.fcrepo.services.ObjectService;
import org.fcrepo.services.functions.GetBinaryStore;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap.Builder;

@Component
@Path("/rest/describe")
public class FedoraRepository extends AbstractResource {

    private static final Logger logger = getLogger(FedoraRepository.class);

    @Autowired
    private Repository repo;

    @Autowired
    private ObjectService objectService;

    @GET
    @Path("modeshape")
	@Timed
    public Response describeModeshape() throws IOException, RepositoryException {
        final Session session = getAuthenticatedSession();
        logger.debug("Repository name: " + repo.getDescriptor(REP_NAME_DESC));
        final Builder<String, Object> repoproperties = builder();
        for (final String key : repo.getDescriptorKeys()) {
            if (repo.getDescriptor(key) != null) {
                repoproperties.put(key, repo.getDescriptor(key));
            }
        }

        // add in node namespaces
        final Builder<String, String> namespaces = builder();
        namespaces.putAll(objectService.getRepositoryNamespaces(session));
        repoproperties.put("node.namespaces", namespaces.build());

        // add in node types
        final Builder<String, String> nodetypes = builder();
        final NodeTypeIterator i = objectService.getAllNodeTypes(session);
        while (i.hasNext()) {
            final NodeType nt = i.nextNodeType();
            nodetypes.put(nt.getName(), nt.toString());
        }
        repoproperties.put("node.types", nodetypes.build());
        session.logout();
        return ok(repoproperties.build().toString()).build();
    }

    @GET
	@Timed
    @Produces({TEXT_XML, APPLICATION_XML, APPLICATION_JSON})
    public DescribeRepository describe() throws RepositoryException {

        final Session session = getAuthenticatedSession();
        final DescribeRepository description = new DescribeRepository();
        description.repositoryBaseURL = uriInfo.getBaseUri();
        description.sampleOAIURL =
                uriInfo.getBaseUriBuilder().path(OBJECT_PATH + "/123/oai_dc")
                        .build();
        description.repositorySize = objectService.getRepositorySize(session);
        description.numberOfObjects =
                objectService.getRepositoryObjectCount(session);
        
        description.clusterConfiguration = getClusterConfig();
    	
        session.logout();
        return description;
    }
    
    /**
     * Gets the cluster configurations
     * @return
     */
    public DescribeCluster getClusterConfig() {
    	try {
	    	//get infinispan binarystore and cachemanager to set cluster configuration information
	    	GetBinaryStore getBinaryStore = new GetBinaryStore();
	    	BinaryStore store = getBinaryStore.apply(repo);
	    	InfinispanBinaryStore ispnStore =
	                (InfinispanBinaryStore) store;
	    	
	    	//seems like we have to start it, not sure why.
	    	ispnStore.start();
	    	
	    	List<Cache<?, ?>> input = ispnStore.getCaches();
	    	DefaultCacheManager cm = (DefaultCacheManager) input
	    			.get(0)
	    			.getCacheManager();
	    	
	    	int nodeView = cm.getTransport()
	    			.getViewId() + 1;
	    	
	    	DescribeCluster clusterConfig = new DescribeCluster();
	    	
	    	clusterConfig.setCacheMode(cm.getCache()
	    			.getCacheConfiguration()
	    			.clustering()
	    			.cacheMode().toString());
	    	clusterConfig.clusterName = cm.getClusterName();
	    	clusterConfig.nodeAddress = cm.getNodeAddress();
	    	clusterConfig.physicalAddress = cm.getPhysicalAddresses();
	    	clusterConfig.nodeView = nodeView;
	    	clusterConfig.clusterSize = cm.getClusterSize();
	    	clusterConfig.clusterMembers = cm.getClusterMembers();
	    	
	    	
	    	ispnStore.shutdown();
	    	return clusterConfig;
    	} catch (Exception e) {
    		logger.debug("Could not get cluster configuration information");
    	} 
    	return null;
    }

    @GET
	@Timed
    @Produces(TEXT_HTML)
    public String describeHtml() throws RepositoryException {

        final VelocityViewer view = new VelocityViewer();
        return view.getRepoInfo(describe());
    }

    /**
     * A testing convenience setter for otherwise injected resources
     * @param repo
     */
    public void setRepository(final Repository repo) {
        this.repo = repo;
    }

    
    public void setObjectService(ObjectService objectService) {
        this.objectService = objectService;
    }

}
