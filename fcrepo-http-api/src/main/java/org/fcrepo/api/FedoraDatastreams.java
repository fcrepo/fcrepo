
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.utils.ContentDigest;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.BodyPartEntity;
import com.sun.jersey.multipart.MultiPart;

@Component
@Path("/rest/{path: .*}/fcr:datastreams")
public class FedoraDatastreams extends AbstractResource {

    private final Logger logger = getLogger(FedoraDatastreams.class);

    @POST
    @Timed
    public Response modifyDatastreams(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("delete")
    final List<String> dsidList, final MultiPart multipart)
            throws RepositoryException, IOException, InvalidChecksumException {

        final Session session = getAuthenticatedSession();
        final String path = toPath(pathList);
        try {
            for (final String dsid : dsidList) {
                logger.debug("Purging datastream: " + dsid);
                nodeService.deleteObject(session, path + "/" + dsid);
            }

            for (final BodyPart part : multipart.getBodyParts()) {
                final String dsid =
                        part.getContentDisposition().getParameters()
                                .get("name");
                logger.debug("Adding datastream: " + dsid);
                final String dsPath = path + "/" + dsid;
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
    @Timed
    public Response deleteDatastreams(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("dsid")
    final List<String> dsidList) throws RepositoryException {
        final Session session = getAuthenticatedSession();
        try {
            final String path = toPath(pathList);
            for (final String dsid : dsidList) {
                logger.debug("purging datastream {}", path + "/" + dsid);
                nodeService.deleteObject(session, path + "/" + dsid);
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
    @Timed
    public Response getDatastreamsContents(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("dsid")
    final List<String> requestedDsids, @Context
    final Request request) throws RepositoryException, IOException,
            NoSuchAlgorithmException {

        final Session session = getAuthenticatedSession();

        final ArrayList<Datastream> datastreams = new ArrayList<Datastream>();

        try {
            final String path = toPath(pathList);
            // TODO: wrap some of this JCR logic in an fcrepo abstraction;

            final Node node = nodeService.getObject(session, path).getNode();

            Date date = new Date();

            final MessageDigest digest = MessageDigest.getInstance("SHA-1");

            final NodeIterator ni;

            if (requestedDsids.isEmpty()) {
                ni = node.getNodes();
            } else {
                ni =
                        node.getNodes(requestedDsids
                                .toArray(new String[requestedDsids.size()]));
            }

            // transform the nodes into datastreams, and calculate cache header data
            while (ni.hasNext()) {

                final Node dsNode = ni.nextNode();
                final Datastream ds = datastreamService.asDatastream(dsNode);

                if (!ds.hasContent()) {
                    continue;
                }

                digest.update(ds.getContentDigest().toString().getBytes());

                if (ds.getLastModifiedDate().after(date)) {
                    date = ds.getLastModifiedDate();
                }

                datastreams.add(ds);
            }

            final URI digestURI =
                    ContentDigest.asURI(digest.getAlgorithm(), digest.digest());
            final EntityTag etag = new EntityTag(digestURI.toString());

            final Date roundedDate = new Date();
            roundedDate.setTime(date.getTime() - date.getTime() % 1000);

            Response.ResponseBuilder builder =
                    request.evaluatePreconditions(roundedDate, etag);

            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);

            if (builder == null) {
                final MultiPart multipart = new MultiPart();

                for (final Datastream ds : datastreams) {
                    multipart.bodyPart(ds.getContent(), MediaType.valueOf(ds
                            .getMimeType()));
                }

                builder = Response.ok(multipart, MULTIPART_FORM_DATA);
            }

            return builder.cacheControl(cc).lastModified(date).tag(etag)
                    .build();

        } finally {
            session.logout();
        }
    }

    public DatastreamService getDatastreamService() {
        return datastreamService;
    }

    public void setDatastreamService(final DatastreamService datastreamService) {
        this.datastreamService = datastreamService;
    }
}
