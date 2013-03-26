
package org.fcrepo.api;

import static com.google.common.collect.ImmutableSet.builder;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
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
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.LoginException;
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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams.DatastreamElement;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.jaxb.responses.management.DatastreamHistory;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.utils.DatastreamIterator;
import org.fcrepo.utils.FixityResult;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet.Builder;

@Path("/objects/{pid}/datastreams")
public class FedoraDatastreams extends AbstractResource {

    final private Logger logger = getLogger(FedoraDatastreams.class);

    @Inject
    DatastreamService datastreamService;

    /**
     * Returns a list of datastreams for the object
     *
     * @param pid
     *            persistent identifier of the digital object
     * @return the list of datastreams
     * @throws RepositoryException
     * @throws IOException
     * @throws TemplateException
     */

    @GET
    @Path("/")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public ObjectDatastreams getDatastreams(@PathParam("pid")
    final String pid) throws RepositoryException, IOException {

        final ObjectDatastreams objectDatastreams = new ObjectDatastreams();
        final Builder<DatastreamElement> datastreams = builder();

        DatastreamIterator i = datastreamService.getDatastreamsFor(pid);
        while (i.hasNext()) {
            final Datastream ds = i.nextDatastream();
            datastreams.add(new DatastreamElement(ds.getDsId(), ds.getDsId(),
                    ds.getMimeType()));
        }
        objectDatastreams.datastreams = datastreams.build();
        return objectDatastreams;

    }

    @POST
    @Path("/")
    public Response modifyDatastreams(@PathParam("pid")
    final String pid, @QueryParam("delete")
    final List<String> dsidList, final List<Attachment> attachmentList)
            throws RepositoryException, IOException, InvalidChecksumException {

        final Session session = repo.login();
        try {
            for (String dsid : dsidList) {
                logger.debug("purging datastream " + dsid);
                datastreamService.purgeDatastream(session, pid, dsid);
            }

            for (final Attachment a : attachmentList) {
                final String dsid =
                        a.getContentDisposition().getParameter("name");
                logger.debug("adding datastream " + dsid);
                final String dsPath = getDatastreamJcrNodePath(pid, dsid);
                datastreamService.createDatastreamNode(session, dsPath, a
                        .getDataHandler().getContentType(), a.getDataHandler()
                        .getInputStream());

            }
            session.save();
            return created(uriInfo.getAbsolutePath()).build();
        } finally {
            session.logout();
        }
    }

    @DELETE
    @Path("/")
    public Response deleteDatastreams(@PathParam("pid")
    final String pid, @QueryParam("dsid")
    final List<String> dsidList) throws RepositoryException {
        final Session session = repo.login();
        try {
            for (String dsid : dsidList) {
                logger.debug("purging datastream " + dsid);
                datastreamService.purgeDatastream(session, pid, dsid);
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
    public MultipartBody getDatastreamsContents(@PathParam("pid")
    final String pid, @QueryParam("dsid")
    List<String> dsids) throws RepositoryException, IOException {

        if (dsids.isEmpty()) {
            NodeIterator ni = objectService.getObjectNode(pid).getNodes();
            while (ni.hasNext()) {
                dsids.add(ni.nextNode().getName());
            }
        }

        List<Attachment> atts = new LinkedList<Attachment>();

        Iterator<String> i = dsids.iterator();
        while (i.hasNext()) {
            final String dsid = i.next();

            try {
                final Datastream ds =
                        datastreamService.getDatastream(pid, dsid);
                atts.add(new Attachment(ds.getDsId(), ds.getMimeType(), ds
                        .getContent()));
            } catch (PathNotFoundException e) {

            }
        }

        return new MultipartBody(atts, true);
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
     * @throws LoginException 
     */
    @POST
    @Path("/{dsid}")
    public Response addDatastream(@PathParam("pid")
    final String pid, @QueryParam("checksumType")
    final String checksumType, @QueryParam("checksum")
    final String checksum, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    MediaType contentType, InputStream requestBodyStream) throws IOException,
            InvalidChecksumException, RepositoryException {
        if (contentType == null) contentType = APPLICATION_OCTET_STREAM_TYPE;
        final Session session = repo.login();
        try {
            String dsPath = getDatastreamJcrNodePath(pid, dsid);
            logger.info("addDatastream {}", dsPath);
            if (!datastreamService.exists(pid, dsid, session)) {
                datastreamService.createDatastreamNode(session, dsPath,
                        contentType.toString(), requestBodyStream,
                        checksumType, checksum);
            } else {
                datastreamService.createDatastreamNode(session, dsPath,
                        contentType.toString(), requestBodyStream,
                        checksumType, checksum);
            }
            session.save();
            return created(uriInfo.getRequestUri()).build();
        } finally {
            session.logout();
        }

    }

    /**
     * Create a new datastream
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
    public Response addDatastream(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    MediaType contentType, InputStream requestBodyStream)
            throws RepositoryException, IOException, InvalidChecksumException {

        return addDatastream(pid, null, null, dsid, contentType,
                requestBodyStream);

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
    public Response modifyDatastream(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    MediaType contentType, InputStream requestBodyStream)
            throws RepositoryException, IOException, InvalidChecksumException {
        final Session session = repo.login();
        try {
            contentType =
                    contentType != null ? contentType
                            : APPLICATION_OCTET_STREAM_TYPE;
            String dsPath = getDatastreamJcrNodePath(pid, dsid);

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
    public DatastreamProfile getDatastream(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsId) throws RepositoryException, IOException {
        logger.trace("Executing getDatastream() with dsId: " + dsId);
        return getDSProfile(datastreamService.getDatastream(pid, dsId));

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
    public Response getDatastreamContent(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid, @Context
    Request request) throws RepositoryException {

        final Datastream ds = datastreamService.getDatastream(pid, dsid);

        EntityTag etag = new EntityTag(ds.getContentDigest().toString());
        Date date = ds.getLastModifiedDate();
        Date rounded_date = new Date();
        rounded_date.setTime(date.getTime() - (date.getTime() % 1000));
        ResponseBuilder builder =
                request.evaluatePreconditions(rounded_date, etag);

        CacheControl cc = new CacheControl();
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
    public DatastreamHistory getDatastreamHistory(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsId) throws RepositoryException, IOException {
        // TODO implement this after deciding on a versioning model
        final Datastream ds = datastreamService.getDatastream(pid, dsId);
        final DatastreamHistory dsHistory =
                new DatastreamHistory(singletonList(getDSProfile(ds)));
        dsHistory.dsID = dsId;
        dsHistory.pid = pid;
        return dsHistory;
    }

    /**
     * Get previous version information for this datastream. See
     * /{dsid}/versions. Kept for compatibility with fcrepo <3.5 API.
     *
     * @deprecated
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
    @Path("/{dsid}/history")
    @Produces(TEXT_XML)
    @Deprecated
    public DatastreamHistory getDatastreamHistoryOld(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid) throws RepositoryException, IOException {
        return getDatastreamHistory(pid, dsid);
    }

    @GET
    @Path("/{dsid}/fixity")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public DatastreamFixity getDatastreamFixity(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid) throws RepositoryException {

        final Datastream ds = datastreamService.getDatastream(pid, dsid);

        DatastreamFixity dsf = new DatastreamFixity();
        dsf.objectId = pid;
        dsf.dsId = dsid;
        dsf.timestamp = new Date();

        Collection<FixityResult> blobs = ds.runFixityAndFixProblems();
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
    public Response deleteDatastream(@PathParam("pid")
    String pid, @PathParam("dsid")
    String dsid) throws RepositoryException {
        final Session session = repo.login();
        try {
            datastreamService.purgeDatastream(session, pid, dsid);
            session.save();
            return noContent().build();
        } finally {
            session.logout();
        }
    }

    private DatastreamProfile getDSProfile(Datastream ds)
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
        dsProfile.dsChecksumType = ds.getContentDigestType();
        dsProfile.dsChecksum = ds.getContentDigest();
        dsProfile.dsState = A;
        dsProfile.dsMIME = ds.getMimeType();
        dsProfile.dsSize = ds.getSize();
        dsProfile.dsCreateDate = ds.getCreatedDate().toString();
        return dsProfile;
    }

}
