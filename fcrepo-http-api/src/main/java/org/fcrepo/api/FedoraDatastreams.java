
package org.fcrepo.api;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Iterators.transform;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.fcrepo.jaxb.responses.management.DatastreamProfile.DatastreamStates.A;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams.DatastreamElement;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.jaxb.responses.management.DatastreamHistory;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.utils.FixityResult;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.BodyPartEntity;
import com.sun.jersey.multipart.MultiPart;

@Component
@Path("/rest/{path: .*(?!(fcr\\:))}/fcr:datastreams")
public class FedoraDatastreams extends AbstractResource {

    private final Logger logger = getLogger(FedoraDatastreams.class);

    @Autowired
    private DatastreamService datastreamService;

    @Autowired
    private LowLevelStorageService llStoreService;

    /**
     * Returns a list of datastreams for the object
     *
     * @param pid
     *            persistent identifier of the digital object
     * @return the list of datastreams
     * @throws RepositoryException
     * @throws IOException
     */

    @GET
    @Path("/")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public ObjectDatastreams getDatastreams(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException, IOException {

        final String path = toPath(pathList);
        logger.info("getting datastreams of {}", path);
        final ObjectDatastreams objectDatastreams = new ObjectDatastreams();

        objectDatastreams.datastreams =
                copyOf(transform(datastreamService
                        .getDatastreamsForPath(path), ds2dsElement));

        return objectDatastreams;

    }

    @POST
    @Path("/")
    public Response modifyDatastreams(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("delete")
    final List<String> dsidList, final MultiPart multipart)
            throws RepositoryException, IOException, InvalidChecksumException {

        final Session session = getAuthenticatedSession();
        final String path = toPath(pathList);
        try {
            for (final String dsid : dsidList) {
                logger.debug("Purging datastream: " + dsid);
                datastreamService.purgeDatastream(session, path, dsid);
            }

            for (final BodyPart part : multipart.getBodyParts()) {
                final String dsid =
                        part.getContentDisposition().getParameters()
                                .get("name");
                logger.debug("Adding datastream: " + dsid);
                final String dsPath = getDatastreamJcrNodePath(path, dsid);
                final Object obj = part.getEntity();
                InputStream src = null;
                if (obj instanceof BodyPartEntity) {
                    final BodyPartEntity entity =
                            (BodyPartEntity) part.getEntity();
                    src = entity.getInputStream();
                } else if (obj instanceof InputStream) {
                    src = (InputStream) obj;
                }
                datastreamService.createDatastreamNode(session, dsPath, part
                        .getMediaType().toString(), src);
            }

            session.save();
            return created(uriInfo.getRequestUri()).build();
        } finally {
            session.logout();
        }
    }

    @DELETE
    @Path("/")
    public Response deleteDatastreams(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("dsid")
    final List<String> dsidList) throws RepositoryException {
        final Session session = getAuthenticatedSession();
        try {
            String path = toPath(pathList);
            for (final String dsid : dsidList) {
                logger.debug("purging datastream {}{}", path, dsid);
                datastreamService.purgeDatastream(session, path, dsid);
            }
            session.save();
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    @GET
    @Path("/__content__")
    @Produces("multipart/mixed")
    public Response getDatastreamsContents(@PathParam("path")
    List<PathSegment> pathList, @QueryParam("dsid")
    final List<String> dsids) throws RepositoryException, IOException {
        
        String path = toPath(pathList);
        if (dsids.isEmpty()) {
            final NodeIterator ni = objectService.getObjectByPath(path).getNode().getNodes();
            while (ni.hasNext()) {
                dsids.add(ni.nextNode().getName());
            }
        }

        final MultiPart multipart = new MultiPart();

        final Iterator<String> i = dsids.iterator();
        while (i.hasNext()) {
            final String dsid = i.next();

            try {
                final Datastream ds =
                        datastreamService.getDatastream(path, dsid);
                multipart.bodyPart(ds.getContent(), MediaType.valueOf(ds
                        .getMimeType()));
            } catch (final PathNotFoundException e) {

            }
        }
        return Response.ok(multipart, MULTIPART_FORM_DATA).build();
    }

    /**
     * Create a new datastream with user provided checksum for validation
     *
     * @param pid
     *            persistent identifier of the digital object
     * @param dsid
     *            datastream identifier
     * @param contentType
     *            Content-Type header
     * @param requestBodyStream
     *            Binary blob
     * @return 201 Created
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException
     */
    @POST
    @Path("/{dsid}")
    public Response addDatastream(@PathParam("path")
    List<PathSegment> pathList, @QueryParam("checksumType")
    final String checksumType, @QueryParam("checksum")
    final String checksum, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    final MediaType requestContentType, final InputStream requestBodyStream)
            throws IOException, InvalidChecksumException, RepositoryException {
        final MediaType contentType =
                requestContentType != null ? requestContentType
                        : APPLICATION_OCTET_STREAM_TYPE;

        final Session session = getAuthenticatedSession();
        try {
            String path = toPath(pathList);
            final String dsPath = getDatastreamJcrNodePath(path, dsid);
            logger.debug("addDatastream {}", dsPath);
            datastreamService.createDatastreamNode(session, dsPath, contentType
                    .toString(), requestBodyStream, checksumType, checksum);
            session.save();
            return created(uriInfo.getRequestUri()).build();
        } finally {
            session.logout();
        }

    }

    /**
     * Modify an existing datastream's content
     *
     * @param pid
     *            persistent identifier of the digital object
     * @param dsid
     *            datastream identifier
     * @param contentType
     *            Content-Type header
     * @param requestBodyStream
     *            Binary blob
     * @return 201 Created
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException 
     */
    @PUT
    @Path("/{dsid}")
    public Response modifyDatastream(@PathParam("path")
    List<PathSegment> pathList, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    final MediaType requestContentType, final InputStream requestBodyStream)
            throws RepositoryException, IOException, InvalidChecksumException {
        final Session session = getAuthenticatedSession();
        try {
            String path = toPath(pathList);
            final MediaType contentType =
                    requestContentType != null ? requestContentType
                            : APPLICATION_OCTET_STREAM_TYPE;
            final String dsPath = getDatastreamJcrNodePath(path, dsid);

            datastreamService.createDatastreamNode(session, dsPath, contentType
                    .toString(), requestBodyStream);
            session.save();
            return created(uriInfo.getRequestUri()).build();
        } finally {
            session.logout();
        }

    }

    /**
     * Get the datastream profile of a datastream
     *
     * @param pid
     *            persistent identifier of the digital object
     * @param dsid
     *            datastream identifier
     * @return 200
     * @throws RepositoryException
     * @throws IOException
     * @throws TemplateException
     */
    @GET
    @Path("/{dsid}")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public DatastreamProfile getDatastream(@PathParam("path")
    List<PathSegment> pathList, @PathParam("dsid")
    final String dsId) throws RepositoryException, IOException {
        String path = toPath(pathList);
        logger.trace("Executing getDatastream() with dsId: " + dsId);
        return getDSProfile(datastreamService.getDatastream(path, dsId));

    }

    /**
     * Get the binary content of a datastream
     *
     * @param pid
     *            persistent identifier of the digital object
     * @param dsid
     *            datastream identifier
     * @return Binary blob
     * @throws RepositoryException
     */
    @GET
    @Path("/{dsid}/content")
    public Response getDatastreamContent(@PathParam("path")
    List<PathSegment> pathList, @PathParam("dsid")
    final String dsid, @Context
    final Request request) throws RepositoryException {

        String path = toPath(pathList);
        final Datastream ds = datastreamService.getDatastream(path, dsid);

        final EntityTag etag = new EntityTag(ds.getContentDigest().toString());
        final Date date = ds.getLastModifiedDate();
        final Date roundedDate = new Date();
        roundedDate.setTime(date.getTime() - date.getTime() % 1000);
        ResponseBuilder builder =
                request.evaluatePreconditions(roundedDate, etag);

        final CacheControl cc = new CacheControl();
        cc.setMaxAge(0);
        cc.setMustRevalidate(true);

        if (builder == null) {
            builder = Response.ok(ds.getContent(), ds.getMimeType());
        }

        return builder.cacheControl(cc).lastModified(date).tag(etag).build();
    }

    /**
     * Get previous version information for this datastream
     *
     * @param pid
     *            persistent identifier of the digital object
     * @param dsId
     *            datastream identifier
     * @return 200
     * @throws RepositoryException
     * @throws IOException
     * @throws TemplateException
     */
    @GET
    @Path("/{dsid}/versions")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public DatastreamHistory getDatastreamHistory(@PathParam("path")
    List<PathSegment> pathList, @PathParam("dsid")
    final String dsId) throws RepositoryException, IOException {
        String path = toPath(pathList);
        // TODO implement this after deciding on a versioning model
        final Datastream ds = datastreamService.getDatastream(path, dsId);
        final DatastreamHistory dsHistory =
                new DatastreamHistory(singletonList(getDSProfile(ds)));
        dsHistory.dsID = dsId;
        dsHistory.pid = pathList.get(pathList.size() - 1).getPath();
        return dsHistory;
    }

    @GET
    @Path("/{dsid}/fixity")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public DatastreamFixity getDatastreamFixity(@PathParam("path")
    List<PathSegment> pathList, @PathParam("dsid")
    final String dsid) throws RepositoryException {

        final String path = toPath(pathList);
        final Datastream ds = datastreamService.getDatastream(path, dsid);

        final DatastreamFixity dsf = new DatastreamFixity();
        dsf.objectId = pathList.get(pathList.size() - 1).getPath();
        dsf.dsId = dsid;
        dsf.timestamp = new Date();

        final Collection<FixityResult> blobs =
                llStoreService.runFixityAndFixProblems(ds);
        dsf.statuses = new ArrayList<FixityResult>(blobs);
        return dsf;
    }

    /**
     * Purge the datastream
     *
     * @param pid
     *            persistent identifier of the digital object
     * @param dsid
     *            datastream identifier
     * @return 204
     * @throws RepositoryException
     */
    @DELETE
    @Path("/{dsid}")
    public Response deleteDatastream(@PathParam("path")
    List<PathSegment> pathList, @PathParam("dsid")
    final String dsid) throws RepositoryException {
        
        final Session session = getAuthenticatedSession();
        final String path = toPath(pathList);
        try {
            datastreamService.purgeDatastream(session, path, dsid);
            session.save();
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    private DatastreamProfile getDSProfile(final Datastream ds)
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
        dsProfile.dsState = A;
        dsProfile.dsMIME = ds.getMimeType();
        dsProfile.dsSize = ds.getSize();
        dsProfile.dsCreateDate = ds.getCreatedDate().toString();
        return dsProfile;
    }

    private Function<Datastream, DatastreamElement> ds2dsElement =
            new Function<Datastream, DatastreamElement>() {

                @Override
                public DatastreamElement apply(Datastream ds) {
                    try {
                        return new DatastreamElement(ds.getDsId(),
                                ds.getDsId(), ds.getMimeType());
                    } catch (RepositoryException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };

    
    public DatastreamService getDatastreamService() {
        return datastreamService;
    }

    
    public void setDatastreamService(DatastreamService datastreamService) {
        this.datastreamService = datastreamService;
    }

    
    public LowLevelStorageService getLlStoreService() {
        return llStoreService;
    }

    
    public void setLlStoreService(LowLevelStorageService llStoreService) {
        this.llStoreService = llStoreService;
    }

}
