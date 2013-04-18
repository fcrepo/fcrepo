
package org.fcrepo.api.legacy;

import static com.google.common.collect.ImmutableSet.builder;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.api.legacy.FedoraObjects.getObjectSize;
import static org.fcrepo.jaxb.responses.management.DatastreamProfile.convertDateToXSDString;
import static org.fcrepo.jaxb.responses.management.DatastreamProfile.DatastreamControlGroup.M;
import static org.fcrepo.jaxb.responses.management.DatastreamProfile.DatastreamStates.A;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.fcrepo.services.ServiceHelpers.getNodePropertySize;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams.DatastreamElement;
import org.fcrepo.jaxb.responses.management.DatastreamHistory;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.ObjectService;
import org.modeshape.jcr.api.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet.Builder;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.multipart.MultiPart;

@Path("/v3/objects/{pid}/datastreams")
@Component("fedoraLegacyDatastreams")
public class FedoraDatastreams extends AbstractResource {

    final private Logger logger = LoggerFactory
            .getLogger(FedoraDatastreams.class);

    @Autowired
    ObjectService objectService;

    @Autowired
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

        NodeIterator i = objectService.getObjectNode(pid).getNodes();
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
    final String pid, final MultiPart multipart)
            throws RepositoryException, IOException, InvalidChecksumException {

        final Session session = getAuthenticatedSession();
        InputStream src = null;
        try {
            Long oldObjectSize =
                    getObjectSize(session.getNode(getObjectJcrNodePath(pid)));

            for (BodyPart part : multipart.getBodyParts()) {
                final String dsid = part.getContentDisposition().getParameters().get("name");
                final String dsPath = getDatastreamJcrNodePath(pid, dsid);
                src = part.getEntityAs(InputStream.class);
                datastreamService.createDatastreamNode(session, dsPath, part.getMediaType().toString(), src);
            }
            
            session.save();

        } finally {
            session.logout();
        }

        return created(uriInfo.getAbsolutePath()).build();
    }

    @GET
    @Path("/__content__")
    @Produces("multipart/mixed")
    public Response getDatastreamsContents(@PathParam("pid")
    final String pid, @QueryParam("dsid")
    List<String> dsids) throws RepositoryException, IOException {

        final Session session = getAuthenticatedSession();

        if (dsids.isEmpty()) {
            NodeIterator ni = objectService.getObjectNode(pid).getNodes();
            while (ni.hasNext()) {
                dsids.add(ni.nextNode().getName());
            }
        }

        try {
            MultiPart multipart = new MultiPart();
            Iterator<String> i = dsids.iterator();
            while (i.hasNext()) {
                final String dsid = i.next();

                try {
                    final Datastream ds =
                            datastreamService.getDatastream(pid, dsid);
                    multipart.bodyPart(ds.getContent(), MediaType.valueOf(ds.getMimeType()));
                } catch (PathNotFoundException e) {

                }
            }
            return Response.ok(multipart,MediaType.MULTIPART_FORM_DATA).build();
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
        final Session session = getAuthenticatedSession();

        contentType =
                contentType != null ? contentType
                        : APPLICATION_OCTET_STREAM_TYPE;
        String dspath = getDatastreamJcrNodePath(pid, dsid);

        if (!session.nodeExists(dspath)) {
            return created(
                    addDatastreamNode(pid, dspath, contentType,
                            requestBodyStream, session)).build();
        } else {
            session.getNode(dspath).remove();
            session.save();
            return created(
                    addDatastreamNode(pid, dspath, contentType,
                            requestBodyStream, session)).build();
        }

    }

    /**
     * Create a new datastream from a multipart/form-data request
     *
     * @param pid
     *            persistent identifier of the digital object
     * @param dsid
     *            datastream identifier
     * @param contentType
     *            Content-Type header
     * @param file
     *            Binary blob
     * @return 201 Created
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException
     */
    @POST
    @Consumes("multipart/form-data")
    @Path("/{dsid}")
    public Response addDatastream(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid, final MultiPart multipart) throws RepositoryException, IOException,
            InvalidChecksumException {
        BodyPart part = multipart.getBodyParts().get(0);
        MediaType type = MediaType.valueOf(part.getHeaders().get("Content-Type").get(0));
        return addDatastream(pid, dsid, type, part.getEntityAs(InputStream.class));
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
        final Session session = getAuthenticatedSession();
        contentType =
                contentType != null ? contentType
                        : APPLICATION_OCTET_STREAM_TYPE;
        String dspath = getDatastreamJcrNodePath(pid, dsid);

        return created(
                addDatastreamNode(pid, dspath, contentType, requestBodyStream,
                        session)).build();

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
     * @param file
     *            Binary blob
     * @return 201 Created
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException
     */
    @PUT
    @Consumes("multipart/form-data")
    @Path("/{dsid}")
    public Response modifyDatastreamMultipart(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsid, @HeaderParam("Content-Type")
    MediaType contentType, @FormDataParam("file") InputStream src) throws RepositoryException, IOException,
            InvalidChecksumException {

        return modifyDatastream(pid, dsid, contentType, src);

    }

    private URI addDatastreamNode(final String pid, final String dsPath,
            final MediaType contentType, final InputStream requestBodyStream,
            final Session session) throws RepositoryException, IOException,
            InvalidChecksumException {

        logger.debug("Attempting to add datastream node at path: " + dsPath);
        try {
            datastreamService.createDatastreamNode(session, dsPath, contentType
                    .toString(), requestBodyStream);
            boolean created = session.nodeExists(dsPath);
            session.save();
        } finally {
            session.logout();
        }
        logger.debug("Finished adding datastream node at path: " + dsPath);
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
        Datastream ds = datastreamService.getDatastream(pid, dsId);
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
    final String dsid) throws RepositoryException {

        final Datastream ds = datastreamService.getDatastream(pid, dsid);
        return ok(ds.getContent(), ds.getMimeType()).build();
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
        final Session session = getAuthenticatedSession();
        final Node ds = session.getNode(dsPath);
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
        // TODO when we have a versioning model, use it here
        dsProfile.dsVersionable = Boolean.toString(false);
        dsProfile.dsVersionID = ds.getDsId() + ".0";
        dsProfile.dsState = A;
        dsProfile.dsControlGroup = M;
        dsProfile.dsChecksumType = ds.getContentDigestType();
        dsProfile.dsChecksum = ds.getContentDigest();
        dsProfile.dsMIME = ds.getMimeType();
        // TODO do something about format URI, or deprecate it
        dsProfile.dsFormatURI = URI.create("info:/nothing");
        dsProfile.dsSize =
                getNodePropertySize(ds.getNode()) + ds.getContentSize();
        dsProfile.dsCreateDate =
                convertDateToXSDString(ds.getCreatedDate().getTime());
        // TODO what _is_ a dsInfoType?
        dsProfile.dsInfoType = "";
        dsProfile.dsLocation = uriInfo.getAbsolutePath().toString();
        dsProfile.dsLocationType = "URL";
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
