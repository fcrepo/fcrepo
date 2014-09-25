/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.hp.hpl.jena.rdf.model.Model;

import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.utils.ContentDigest;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.value.PathFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;

/**
 * Controller for manipulating binary streams in larger batches
 * by using multipart requests and responses
 *
 * @author cbeer
 */
@Scope("prototype")
@Path("/{path: .*}/fcr:batch")
public class FedoraBatch extends AbstractResource {

    public static final String ATTACHMENT = "attachment";
    public static final String INLINE = "inline";
    public static final String DELETE = "delete";
    public static final String FORM_DATA_DELETE_PART_NAME = "delete[]";
    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraBatch.class);

    /**
     * Apply batch modifications relative to the node.
     *
     * This endpoint supports two types of multipart requests:
     *  - mixed (preferred)
     *  - form-data (fallback for "dumb" clients)
     *
     *  The name-part of the multipart request is relative to the node
     *  this operation was called on.
     *
     *  multipart/mixed:
     *
     *  mixed mode supports three content-disposition types:
     *  - inline
     *      Add or update an objects triples
     *  - attachment
     *      Add or update binary content
     *  - delete
     *      Delete an object
     *
     *  multipart/form-data:
     *
     *  form-data is a fallback for dumb clients (e.g. HttpClient, curl, etc).
     *  Instead of using the Content-Disposition to determine what operation
     *  to perform, form-data uses heuristics to figure out what to do.
     *
     *  - if the entity has a filename, always treat it as binary content
     *  - if the content is RDF or SPARQL-Update, add or update triples
     *  - if the entity has the name "delete[]", the body is a single path
     *  to delete.
     *  - otherwise, treat the entity as binary content.
     *
     *
     * @param pathList
     * @param multipart
     * @return response
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException
     */
    @POST
    @Timed
    public Response batchModify(@PathParam("path") final List<PathSegment> pathList,
                                final MultiPart multipart)
        throws InvalidChecksumException, IOException, URISyntaxException {

        final String path = toPath(pathList);

        // TODO: this is ugly, but it works.
        final PathFactory pathFactory = new ExecutionContext().getValueFactories().getPathFactory();
        final org.modeshape.jcr.value.Path jcrPath = pathFactory.create(path);

        try {

            final Set<FedoraResource> resourcesChanged = new HashSet<>();

            // iterate through the multipart entities
            for (final BodyPart part : multipart.getBodyParts()) {
                final ContentDisposition contentDisposition = part.getContentDisposition();


                // a relative path (probably.)

                final String contentDispositionType = contentDisposition.getType();

                final String partName = contentDisposition.getParameters().get("name");

                final String contentTypeString = getSimpleContentType(part.getMediaType()).toString();

                LOGGER.trace("Processing {} part {} with media type {}",
                                contentDispositionType, partName, contentTypeString);

                final String realContentDisposition;

                // we need to apply some heuristics for "dumb" clients that
                // can only send form-data content
                if (contentDispositionType.equals("form-data")) {

                    if (contentDisposition.getFileName() != null) {
                        realContentDisposition = ATTACHMENT;
                    } else if (contentTypeString.equals(contentTypeSPARQLUpdate)
                        || isRdfContentType(contentTypeString)) {
                        realContentDisposition = INLINE;
                    } else if (partName.equals(FORM_DATA_DELETE_PART_NAME)) {
                        realContentDisposition = DELETE;
                    } else {
                        realContentDisposition = ATTACHMENT;
                    }

                    LOGGER.trace("Converted form-data to content disposition {}", realContentDisposition);
                } else {
                    realContentDisposition = contentDispositionType;
                }

                // convert the entity to an InputStream
                final Object entityBody = part.getEntity();

                final InputStream src;
                if (entityBody instanceof BodyPartEntity) {
                    final BodyPartEntity entity =
                        (BodyPartEntity) part.getEntity();
                    src = entity.getInputStream();
                } else if (entityBody instanceof InputStream) {
                    src = (InputStream) entityBody;
                } else {
                    LOGGER.debug("Got unknown multipart entity for {}; ignoring it", partName);
                    src = IOUtils.toInputStream("");
                }

                // convert the entity name to a node path
                final String pathName;

                if (partName.equals(FORM_DATA_DELETE_PART_NAME)) {
                    pathName = IOUtils.toString(src);
                } else {
                    pathName = partName;
                }

                final String objPath = pathFactory.create(jcrPath, pathName).getCanonicalPath().getString();

                switch (realContentDisposition) {
                    case INLINE:

                        final HttpIdentifierTranslator subjects =
                            new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

                        final FedoraResource resource = objectService.findOrCreateObject(session, objPath);

                        if (contentTypeString.equals(contentTypeSPARQLUpdate)) {
                            resource.updatePropertiesDataset(subjects, IOUtils.toString(src));
                        } else if (contentTypeToLang(contentTypeString) != null) {
                            final Lang lang = contentTypeToLang(contentTypeString);

                            final String format = lang.getName().toUpperCase();

                            final Model inputModel =
                                createDefaultModel().read(src,
                                        subjects.getSubject(resource.getPath()).toString(), format);

                            resource.replaceProperties(subjects, inputModel,
                                    resource.getTriples(subjects, PropertiesRdfContext.class));
                        } else {
                            throw new WebApplicationException(notAcceptable(null)
                                .entity("Invalid Content Type " + contentTypeString).build());
                        }

                        resourcesChanged.add(resource);

                        break;

                    case ATTACHMENT:

                        final URI checksumURI;

                        final String checksum = contentDisposition.getParameters().get("checksum");

                        if (checksum != null && !checksum.equals("")) {
                            checksumURI = new URI(checksum);
                        } else {
                            checksumURI = null;
                        }

                        final Datastream datastream = datastreamService.findOrCreateDatastream(session, objPath);

                        datastream.getBinary().setContent(src,
                                part.getMediaType().toString(),
                                checksumURI,
                                contentDisposition.getFileName(),
                                datastreamService.getStoragePolicyDecisionPoint());

                        resourcesChanged.add(datastream);
                        break;

                    case DELETE:
                        nodeService.getObject(session, objPath).delete();
                        break;

                    default:
                        return status(Status.BAD_REQUEST)
                                   .entity("Unknown Content-Disposition: " + realContentDisposition).build();
                }
            }

            try {
                session.save();
                versionService.nodeUpdated(session, path);
                for (final FedoraResource resource : resourcesChanged) {
                    versionService.nodeUpdated(resource.getNode());
                }
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            final HttpIdentifierTranslator subjects =
                    new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

            return created(new URI(subjects.getSubject(path).getURI())).build();

        } finally {
            session.logout();
        }
    }

    /**
     * Retrieve multiple datastream bitstreams in a single request as a
     * multipart/mixed response.
     *
     * @param pathList
     * @param requestedChildren
     * @param request
     * @return response
     * @throws RepositoryException
     * @throws NoSuchAlgorithmException
     */
    @GET
    @Produces("multipart/mixed")
    @Timed
    public Response getBinaryContents(
        @PathParam("path") final List<PathSegment> pathList,
        @QueryParam("child") final List<String> requestedChildren,
        @Context final Request request) throws RepositoryException, NoSuchAlgorithmException {

        final List<Datastream> datastreams = new ArrayList<>();

        try {
            final String path = toPath(pathList);
            // TODO: wrap some of this JCR logic in an fcrepo abstraction;

            final Node node = nodeService.getObject(session, path).getNode();

            Date date = new Date();

            final MessageDigest digest = MessageDigest.getInstance("SHA-1");

            final NodeIterator ni;

            if (requestedChildren.isEmpty()) {
                ni = node.getNodes();
            } else {
                ni =
                        node.getNodes(requestedChildren
                                .toArray(new String[requestedChildren.size()]));
            }

            // complain if no children found
            if ( ni.getSize() == 0 ) {
                return status(Status.BAD_REQUEST).build();
            }

            // transform the nodes into datastreams, and calculate cache header
            // data
            while (ni.hasNext()) {

                final Node dsNode = ni.nextNode();
                final Datastream ds = datastreamService.asDatastream(dsNode);

                if (!ds.hasContent()) {
                    continue;
                }

                final FedoraBinary binary = ds.getBinary();

                digest.update(binary.getContentDigest().toString().getBytes(
                        UTF_8));

                if (binary.getLastModifiedDate().after(date)) {
                    date = binary.getLastModifiedDate();
                }

                datastreams.add(ds);
            }

            final URI digestURI =
                    ContentDigest.asURI(digest.getAlgorithm(), digest.digest());
            final EntityTag etag = new EntityTag(digestURI.toString());

            final Date roundedDate = new Date();
            roundedDate.setTime(date.getTime() - date.getTime() % 1000);

            ResponseBuilder builder =
                    request.evaluatePreconditions(roundedDate, etag);

            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);

            if (builder == null) {
                final MultiPart multipart = new MultiPart();

                for (final Datastream ds : datastreams) {
                    final FedoraBinary binary = ds.getBinary();
                    final BodyPart bodyPart =
                            new BodyPart(binary.getContent(),
                                    MediaType.valueOf(binary.getMimeType()));
                    bodyPart.setContentDisposition(
                            ContentDisposition.type(ATTACHMENT)
                                    .fileName(ds.getPath())
                                    .creationDate(binary.getCreatedDate())
                                    .modificationDate(binary.getLastModifiedDate())
                                    .size(binary.getContentSize())
                            .build());
                    multipart.bodyPart(bodyPart);
                }

                builder = ok(multipart, MULTIPART_FORM_DATA);
            }

            return builder.cacheControl(cc).lastModified(date).tag(etag)
                    .build();

        } finally {
            session.logout();
        }
    }
}
