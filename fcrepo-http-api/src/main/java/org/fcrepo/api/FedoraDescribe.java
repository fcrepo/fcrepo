package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.jaxb.responses.access.DescribeRepository;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.provider.VelocityViewer;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.ObjectService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/{path: .*}/fcr:describe")
public class FedoraDescribe extends AbstractResource {

    private static final Logger logger = getLogger(FedoraDescribe.class);

    @Autowired
    private Repository repo;

    @Autowired
    private ObjectService objectService;

    @Autowired
    private DatastreamService datastreamService;

    @Autowired
    private LowLevelStorageService llStoreService;

    @GET
    @Produces({TEXT_XML, APPLICATION_JSON, TEXT_HTML})
    public Response describe(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException, IOException {

        final String path = toPath(pathList);
        logger.trace("getting profile for {}", path);
        if ("/".equals(path)) {
            return Response.ok(getRepositoryProfile()).build();
        }
        final Session session = getAuthenticatedSession();
        Node node = session.getNode(path);
        if (node.isNodeType("nt:file")) {
            return Response.ok(getDatastreamProfile(node)).build();
        }
        if (node.isNodeType("nt:folder")) {
            return Response.ok(getObjectProfile(node)).build();
        }
        return Response.status(406).entity("Unexpected node type: " + node.getPrimaryNodeType()).build();
    }

    /**
     * Returns an object profile.
     *
     * @param path
     * @return 200
     * @throws RepositoryException
     * @throws IOException
     */
    public ObjectProfile getObjectProfile(Node node)
            throws RepositoryException, IOException {

        final String path = node.getPath();
        logger.trace("getting object profile {}", path);
        final ObjectProfile objectProfile = new ObjectProfile();
        final FedoraObject obj = objectService.getObject(path);
        objectProfile.pid = obj.getName();
        objectProfile.objLabel = obj.getLabel();
        objectProfile.objOwnerId = obj.getOwnerId();
        objectProfile.objCreateDate = obj.getCreated();
        objectProfile.objLastModDate = obj.getLastModified();
        objectProfile.objSize = obj.getSize();
        objectProfile.objItemIndexViewURL =
                uriInfo.getAbsolutePathBuilder().path("datastreams").build();
        objectProfile.objState = ObjectProfile.ObjectStates.A;
        objectProfile.objModels = obj.getModels();
        return objectProfile;

    }

    public ObjectService getObjectService() {
        return objectService;
    }


    public void setObjectService(ObjectService objectService) {
        this.objectService = objectService;
    }

    public DatastreamProfile getDatastreamProfile(Node node) throws RepositoryException, IOException {
        final String path = node.getPath();
        logger.trace("Executing getDatastream() with path: {}", path);
        return getDatastreamProfile(datastreamService.getDatastream(path));

    }

    private DatastreamProfile getDatastreamProfile(final Datastream ds)
            throws RepositoryException, IOException {
        logger.trace("Executing getDSProfile() with node: " + ds.getDsId());
        final DatastreamProfile dsProfile = new DatastreamProfile();
        dsProfile.dsID = ds.getDsId();
        dsProfile.pid = ds.getObject().getName();
        logger.trace("Retrieved datastream " + ds.getDsId() + "'s parent: " +
                dsProfile.pid);
        dsProfile.dsLabel = ds.getLabel();
        logger.trace("Retrieved datastream " + ds.getDsId() + "'s label: " +
                ds.getLabel());
        dsProfile.dsOwnerId = ds.getOwnerId();
        dsProfile.dsChecksumType = ds.getContentDigestType();
        dsProfile.dsChecksum = ds.getContentDigest();
        dsProfile.dsState = DatastreamProfile.DatastreamStates.A;
        dsProfile.dsMIME = ds.getMimeType();
        dsProfile.dsSize = ds.getSize();
        dsProfile.dsCreateDate = ds.getCreatedDate().toString();
        dsProfile.dsStores =  new DatastreamProfile.DSStores(ds,
                llStoreService.getLowLevelCacheEntries(ds.getNode()));
        return dsProfile;
    }

    public DatastreamService getDatastreamService() {
        return datastreamService;
    }


    public void setDatastreamService(DatastreamService datastreamService) {
        this.datastreamService = datastreamService;
    }

    public DescribeRepository getRepositoryProfile() throws RepositoryException {

        final Session session = getAuthenticatedSession();
        final DescribeRepository description = new DescribeRepository();
        description.repositoryBaseURL = uriInfo.getBaseUri();
        description.sampleOAIURL =
                uriInfo.getBaseUriBuilder().path("/123/oai_dc")
                        .build();
        description.repositorySize = objectService.getRepositorySize(session);
        description.numberOfObjects =
                objectService.getRepositoryObjectCount(session);
        session.logout();
        return description;
    }

    //TODO Figure out how to call this
    public String getRepositoryProfileHtml() throws RepositoryException {

        final VelocityViewer view = new VelocityViewer();
        return view.getRepoInfo(getRepositoryProfile());
    }

    public LowLevelStorageService getLlStoreService() {
        return llStoreService;
    }

    public void setLlStoreService(final LowLevelStorageService llStoreService) {
        this.llStoreService = llStoreService;
    }
}
