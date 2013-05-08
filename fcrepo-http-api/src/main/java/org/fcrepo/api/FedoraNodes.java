
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.fcrepo.http.RDFMediaType.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Variant;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.update.UpdateAction;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.jena.riot.WebContent;
import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.jaxb.responses.access.DescribeRepository;
import org.fcrepo.jaxb.responses.access.ObjectProfile;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.provider.GraphStreamingOutput;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.common.collection.Problems;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/{path: .*}")
public class FedoraNodes extends AbstractResource {

    private static final Logger logger = getLogger(FedoraNodes.class);

	@Autowired
	private LowLevelStorageService llStoreService;
	
	/**
	 * Jersey seems to demote methods that @Produces(WILDCARD), even when
	 * no methods match the Accept header (eg, when it's null). This is 
	 * appears to be an interpretation of 
	 * http://jsr311.java.net/nonav/releases/1.1/spec/spec3.html#x3-360003.7.2
	 * So this is just a hack to dispatch to the "real" default.
	 * @param pathList
	 * @return
	 * @throws RepositoryException
	 * @throws IOException
	 */
	@GET
	@Produces(WILDCARD)
	public Response defaultDescribe(@PathParam("path")
							 final List<PathSegment> pathList) throws RepositoryException, IOException {
		return describe(pathList);
	}

	@GET
	@Produces({TEXT_XML, APPLICATION_JSON, APPLICATION_XML})
	public Response describe(@PathParam("path")
							 final List<PathSegment> pathList) throws RepositoryException, IOException {

		final String path = toPath(pathList);
		logger.trace("getting profile for {}", path);
		if ("/".equals(path)) {
			return Response.ok(getRepositoryProfile()).build();
		}
		final Session session = getAuthenticatedSession();
		try {
			Node node = session.getNode(path);

			if (node.isNodeType("nt:file")) {
				return Response.ok(getDatastreamProfile(node)).build();
			}

			if (node.isNodeType("nt:folder")) {
				return Response.ok(getObjectProfile(node)).build();
			}

			return Response.status(406).entity("Unexpected node type: " + node.getPrimaryNodeType()).build();
		} finally {
			session.logout();
		}
	}

	@GET
	@Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
	public StreamingOutput describeRdf(@PathParam("path") final List<PathSegment> pathList, @Context Request request) throws RepositoryException, IOException {

		final String path = toPath(pathList);
		logger.trace("getting profile for {}", path);

		Variant bestPossibleResponse = request.selectVariant(POSSIBLE_RDF_VARIANTS);

		return new GraphStreamingOutput(
				getAuthenticatedSessionProvider(),
				objectService,
				path,
				bestPossibleResponse.getMediaType());

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
		final FedoraObject obj = objectService.getObject(node.getSession(), path);
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

	public DatastreamProfile getDatastreamProfile(Node node) throws RepositoryException, IOException {
		final String path = node.getPath();
		logger.trace("Executing getDatastream() with path: {}", path);
		return getDatastreamProfile(datastreamService.getDatastream(node.getSession(), path));

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

	public DescribeRepository getRepositoryProfile() throws RepositoryException {

		final Session session = getAuthenticatedSession();
		final DescribeRepository description = new DescribeRepository();
		description.repositoryBaseURL = uriInfo.getBaseUri();
		description.sampleOAIURL =
				uriInfo.getBaseUriBuilder().path("/123/oai_dc")
						.build();
		description.repositorySize = objectService.getRepositorySize();
		description.numberOfObjects =
				objectService.getRepositoryObjectCount(session);
		session.logout();
		return description;
	}

	/**
     * Does nothing yet-- must be improved to handle the FCREPO3 PUT to /objects/{pid}
     * 
     * @param pid
     * @return 201
     * @throws RepositoryException
     */
    @PUT
	@Timed
    public Response modifyObject(@PathParam("path")
    final List<PathSegment> pathList, final InputStream requestBodyStream) throws RepositoryException, IOException {
        final Session session = getAuthenticatedSession();
		String path = toPath(pathList);
		logger.debug("Modifying object with path: {}", path);

        try {

			final FedoraObject result =
					objectService.getObject(session, path);

			if (requestBodyStream != null) {
				UpdateAction.parseExecute(IOUtils.toString(requestBodyStream), result.getGraphStore());
			}
            session.save();

			return Response.temporaryRedirect(uriInfo.getRequestUri()).build();
        } finally {
            session.logout();
        }
    }

	/**
	 * Update an object using SPARQL-UPDATE
	 *
	 * @param pathList
	 * @return 201
	 * @throws RepositoryException
	 * @throws org.fcrepo.exception.InvalidChecksumException
	 * @throws IOException
	 */
	@POST
	@Consumes({WebContent.contentTypeSPARQLUpdate})
	@Timed
	public Response updateSparql(
										@PathParam("path") final List<PathSegment> pathList,
										final InputStream requestBodyStream
	) throws RepositoryException, IOException {

		String path = toPath(pathList);
		logger.debug("Attempting to ingest with path: {}", path);

		final Session session = getAuthenticatedSession();

		try {
			if (objectService.exists(session, path)) {

				if(requestBodyStream != null) {

					final FedoraObject result = objectService.getObject(session, path);

					result.updateGraph(IOUtils.toString(requestBodyStream));
					Problems problems = result.getGraphProblems();
					if (problems != null && problems.hasProblems()) {
						logger.info("Found these problems updating the properties for {}: {}", path, problems.toString());
						return Response.status(Response.Status.FORBIDDEN).entity(problems.toString()).build();

					}

					session.save();

					return Response.status(HttpStatus.SC_NO_CONTENT).build();
				} else {
					return Response.status(HttpStatus.SC_BAD_REQUEST).entity("SPARQL-UPDATE requests must have content ").build();
				}
			} else {
				return Response.status(HttpStatus.SC_NOT_FOUND).entity(path + " must be an existing resource").build();
			}

		} finally {
			session.logout();
		}
	}

    /**
     * Creates a new object.
     * 
     * @param pid
     * @return 201
     * @throws RepositoryException
     * @throws InvalidChecksumException 
     * @throws IOException 
     */
    @POST
	@Timed
    public Response createObject(
            @PathParam("path") final List<PathSegment> pathList,
            @QueryParam("label") @DefaultValue("") final String label,
            @QueryParam("mixin") @DefaultValue(FedoraJcrTypes.FEDORA_OBJECT) String mixin,
            @QueryParam("checksumType") final String checksumType,
            @QueryParam("checksum") final String checksum,
            @HeaderParam("Content-Type") final MediaType requestContentType,
            final InputStream requestBodyStream
            ) throws RepositoryException, IOException, InvalidChecksumException {
        
        String path = toPath(pathList);
        logger.debug("Attempting to ingest with path: {}", path);

        final Session session = getAuthenticatedSession();
        
        try {
            if (objectService.exists(session, path)) {
                	return Response.status(HttpStatus.SC_CONFLICT).entity(path + " is an existing resource").build();
            }

            if (FedoraJcrTypes.FEDORA_OBJECT.equals(mixin)){
                final FedoraObject result =
                        objectService.createObject(session, path);
                if (label != null && !"".equals(label)) {
                    result.setLabel(label);
                }

				if(requestBodyStream != null && requestContentType != null && requestContentType.toString().equals(WebContent.contentTypeSPARQLUpdate)) {
					result.updateGraph(IOUtils.toString(requestBodyStream));
				}

            }
            if (FedoraJcrTypes.FEDORA_DATASTREAM.equals(mixin)){
                final MediaType contentType =
                        requestContentType != null ? requestContentType
                                : APPLICATION_OCTET_STREAM_TYPE;
                final Node result =
                datastreamService.createDatastreamNode(session, path, contentType
                        .toString(), requestBodyStream, checksumType, checksum);
                Datastream ds = new Datastream(result);
                ds.setLabel(label);
            }
            session.save();
            logger.debug("Finished creating {} with path: {}", mixin, path);
            return created(uriInfo.getRequestUri()).entity(path).build();

        } finally {
            session.logout();
        }
    }

    /**
     * Deletes an object.
     * 
     * @param pid
     * @return
     * @throws RepositoryException
     */
    @DELETE
	@Timed
    public Response deleteObject(@PathParam("path")
    final List<PathSegment> path) throws RepositoryException {
        final Session session = getAuthenticatedSession();
        objectService.deleteObject(session, toPath(path));
        session.save();
        return noContent().build();
    }

    
    public ObjectService getObjectService() {
        return objectService;
    }

    
    public void setObjectService(ObjectService objectService) {
        this.objectService = objectService;
    }

    public DatastreamService getDatastreamService() {
        return datastreamService;
    }

    
    public void setDatastreamService(DatastreamService datastreamService) {
        this.datastreamService = datastreamService;
    }

	public LowLevelStorageService getLlStoreService() {
		return llStoreService;
	}

	public void setLlStoreService(final LowLevelStorageService llStoreService) {
		this.llStoreService = llStoreService;
	}

}
