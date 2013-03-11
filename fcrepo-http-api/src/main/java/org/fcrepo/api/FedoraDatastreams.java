
package org.fcrepo.api;

import static com.google.common.collect.ImmutableSet.builder;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static org.fcrepo.api.FedoraObjects.getObjectSize;
import static org.fcrepo.jaxb.responses.management.DatastreamProfile.DatastreamStates.A;
import static org.fcrepo.services.DatastreamService.createDatastreamNode;
import static org.fcrepo.services.ObjectService.getObjectNode;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
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
import org.fcrepo.jaxb.responses.management.DatastreamHistory;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.modeshape.jcr.api.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet.Builder;

@Path("/objects/{pid}/datastreams")
public class FedoraDatastreams extends AbstractResource {

    final private Logger logger = LoggerFactory
            .getLogger(FedoraDatastreams.class);

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

        NodeIterator i = getObjectNode(pid).getNodes();
        while (i.hasNext()) {
            final Node ds = i.nextNode();
            datastreams.add(new DatastreamElement(ds.getName(), ds.getName(),
                    getDSMimeType(ds)));
        }
        objectDatastreams.datastreams = datastreams.build();
        return objectDatastreams;

    }

    @POST
    @Path("/")
    public Response addDatastreams(@PathParam("pid")
    final String pid, final List<Attachment> attachmentList)
            throws RepositoryException, IOException, InvalidChecksumException {

        final Session session = repo.login();
        try {
            Long oldObjectSize =
                    getObjectSize(session.getNode(getObjectJcrNodePath(pid)));

            for (final Attachment a : attachmentList) {
                final String dsid =
                        a.getContentDisposition().getParameter("name");
                final String dsPath = getDatastreamJcrNodePath(pid, dsid);
                createDatastreamNode(session, dsPath, a.getDataHandler()
                        .getContentType(), a.getDataHandler().getInputStream());

            }
            session.save();

            /*
             * we save before updating the repo size because the act of
             * persisting session state creates new system-curated nodes and
             * properties which contribute to the footprint of this resource
             */
            updateRepositorySize(getObjectSize(session
                    .getNode(getObjectJcrNodePath(pid))) -
                    oldObjectSize, session);
            // now we save again to persist the repo size
            session.save();
        } finally {
            session.logout();
        }

        return created(uriInfo.getAbsolutePath()).build();
    }

    @GET
    @Path("/__content__")
    @Produces("multipart/mixed")
    public MultipartBody getDatastreamsContents(@PathParam("pid")
    final String pid, @QueryParam("dsid")
    List<String> dsids) throws RepositoryException, IOException {

        final Session session = repo.login();

        if (dsids.isEmpty()) {
            NodeIterator ni = getObjectNode(pid).getNodes();
            while (ni.hasNext()) {
                dsids.add(ni.nextNode().getName());
            }
        }

        List<Attachment> atts = new LinkedList<Attachment>();
        try {
            Iterator<String> i = dsids.iterator();
            while (i.hasNext()) {
                final String dsid = i.next();

                try {
                    final Datastream ds =
                            DatastreamService.getDatastream(pid, dsid);
                    atts.add(new Attachment(ds.getNode().getName(), ds
                            .getMimeType(), ds.getContent()));
                } catch (PathNotFoundException e) {

                }
            }
        } finally {
            session.logout();
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
     */
    @POST
    @Path("/{dsid}")
    public Response addDatastream(@PathParam("pid")
    final String pid, @QueryParam("checksumType")
    final String checksumType, @QueryParam("checksum")
    final String checksum, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    MediaType contentType, InputStream requestBodyStream)
            throws RepositoryException, IOException {
        final Session session = repo.login();

        contentType =
                contentType != null ? contentType
                        : APPLICATION_OCTET_STREAM_TYPE;
        String dspath = getDatastreamJcrNodePath(pid, dsid);

        if (!session.nodeExists(dspath)) {
            return created(
                    addDatastreamNode(pid, dspath, contentType,
                            requestBodyStream, session, checksumType, checksum)).build();
        } else {
            session.getNode(dspath).remove();
            session.save();
            return created(
                    addDatastreamNode(pid, dspath, contentType,
                            requestBodyStream, session, checksumType, checksum)).build();
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
     */
    @POST
    @Path("/{dsid}")
    public Response addDatastream(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    MediaType contentType, InputStream requestBodyStream)
            throws RepositoryException, IOException {
    	
    	return addDatastream(pid, null, null, dsid, contentType, requestBodyStream);

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
     */
    @PUT
    @Path("/{dsid}")
    public Response modifyDatastream(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    MediaType contentType, InputStream requestBodyStream)
            throws RepositoryException, IOException {
        final Session session = repo.login();
        contentType =
                contentType != null ? contentType
                        : APPLICATION_OCTET_STREAM_TYPE;
        String dspath = getDatastreamJcrNodePath(pid, dsid);

        return created(
                addDatastreamNode(pid, dspath, contentType, requestBodyStream,
                        session, null, null)).build();

    }

    private URI addDatastreamNode(final String pid, final String dsPath,
            final MediaType contentType, final InputStream requestBodyStream,
            final Session session, String checksumType, String checksum) throws RepositoryException, IOException {

        Long oldObjectSize =
                getObjectSize(session.getNode(getObjectJcrNodePath(pid)));
        logger.debug("Attempting to add datastream node at path: " + dsPath);
        try {
            boolean created = session.nodeExists(dsPath);
            createDatastreamNode(session, dsPath, contentType.toString(),
                    requestBodyStream, checksumType, checksum);
            session.save();
            if (created) {
                /*
                 * we save before updating the repo size because the act of
                 * persisting session state creates new system-curated nodes and
                 * properties which contribute to the footprint of this resource
                 */
                updateRepositorySize(getObjectSize(session
                        .getNode(getObjectJcrNodePath(pid))) -
                        oldObjectSize, session);
                // now we save again to persist the repo size
                session.save();
            }
            logger.debug("Finished adding datastream node at path: " + dsPath);
        } catch (InvalidChecksumException e) { 
        	logger.error("Checksum Mismatch Exception");
        	logger.debug("No datastream has been added");
        	session.logout();
        } finally {
            session.logout();
        }
        return uriInfo.getAbsolutePath();
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
        Datastream ds = DatastreamService.getDatastream(pid, dsId);
        logger.debug("Retrieved dsNode: " + ds.getNode().getName());
        return getDSProfile(ds);

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
    final String dsid, @Context Request request) throws RepositoryException {

        final Datastream ds = DatastreamService.getDatastream(pid, dsid);

        EntityTag etag = new EntityTag(ds.getContentDigest().toString());
        Date date = ds.getLastModifiedDate();
//        ResponseBuilder builder = request.evaluatePreconditions(date, etag);

        ResponseBuilder builder = request.evaluatePreconditions(etag);

        CacheControl cc = new CacheControl();
        cc.setMaxAge(0);
        cc.setMustRevalidate(true);

        if(builder == null) {
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
    // TODO implement this after deciding on a versioning model
            public
            DatastreamHistory getDatastreamHistory(@PathParam("pid")
            final String pid, @PathParam("dsid")
            final String dsId) throws RepositoryException, IOException {

        final Datastream ds = DatastreamService.getDatastream(pid, dsId);
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
        final String dsPath = getDatastreamJcrNodePath(pid, dsid);
        final Session session = repo.login();
        final Node ds = session.getNode(dsPath);
        updateRepositorySize(0L - getDatastreamSize(ds), session);
        return deleteResource(ds);
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
        dsProfile.dsSize =
                getNodePropertySize(ds.getNode()) + ds.getContentSize();
        dsProfile.dsCreateDate = ds.getCreatedDate().toString();
        return dsProfile;
    }

    private String getDSMimeType(Node ds) throws ValueFormatException,
            PathNotFoundException, RepositoryException, IOException {
        final Binary b =
                (Binary) ds.getNode(JCR_CONTENT).getProperty(JCR_DATA)
                        .getBinary();
        return b.getMimeType();
    }

    public static Long getDatastreamSize(Node ds) throws ValueFormatException,
            PathNotFoundException, RepositoryException {
        return getNodePropertySize(ds) + getContentSize(ds);
    }

    public static Long getContentSize(Node ds) throws ValueFormatException,
            PathNotFoundException, RepositoryException {
        return ds.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
                .getSize();
    }

}
