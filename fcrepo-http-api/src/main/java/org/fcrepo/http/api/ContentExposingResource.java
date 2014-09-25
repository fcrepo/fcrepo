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

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.fcrepo.http.api.versioning.VersionAwareHttpIdentifierTranslator;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.domain.Prefer;
import org.fcrepo.http.commons.domain.PreferTag;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.impl.FedoraObjectImpl;
import org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.LdpContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.impl.services.TransactionServiceImpl;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.slf4j.Logger;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.transform;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static javax.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static javax.ws.rs.core.Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An abstract class that sits between AbstractResource and any resource that
 * wishes to share the routines for building responses containing binary
 * content.
 *
 * @author Mike Durbin
 */
public abstract class ContentExposingResource extends AbstractResource {

    private static final Logger LOGGER = getLogger(ContentExposingResource.class);

    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

    protected FedoraResource resource;
    protected HttpIdentifierTranslator identifierTranslator;

    private static long MAX_BUFFER_SIZE = 10240000;

    abstract Session session();

    abstract void addResourceHttpHeaders(FedoraResource resource);

    protected Response getContent(final Prefer prefer,
                                  final String rangeValue,
                                  final RdfStream rdfStream) throws IOException {
        if (resource() instanceof FedoraBinary) {

            final String contentTypeString = ((FedoraBinary) resource()).getMimeType();

            final Lang lang = contentTypeToLang(contentTypeString);

            if (!contentTypeString.equals("text/plain") && lang != null) {

                final String format = lang.getName().toUpperCase();

                final InputStream content = ((FedoraBinary) resource()).getContent();

                final Model inputModel = createDefaultModel()
                        .read(content, getUri(resource()).toString(), format);

                rdfStream.concat(Iterators.transform(inputModel.listStatements(),
                        new Function<Statement, Triple>() {

                            @Override
                            public Triple apply(final Statement input) {
                                return input.asTriple();
                            }
                        }));
            } else {
                return getBinaryContent(rangeValue);
            }

        } else {

            rdfStream.concat(getTriples(PropertiesRdfContext.class));

            final PreferTag returnPreference;

            if (prefer != null && prefer.hasReturn()) {
                returnPreference = prefer.getReturn();
            } else {
                returnPreference = new PreferTag("");
            }

            if (!returnPreference.getValue().equals("minimal")) {
                final LdpPreferTag ldpPreferences = new LdpPreferTag(returnPreference);

                if (ldpPreferences.prefersReferences()) {
                    rdfStream.concat(getTriples(ReferencesRdfContext.class));
                }

                rdfStream.concat(getTriples(ParentRdfContext.class));

                if (ldpPreferences.prefersContainment() || ldpPreferences.prefersMembership()) {
                    rdfStream.concat(getTriples(ChildrenRdfContext.class));
                }

                if (ldpPreferences.prefersMembership()) {
                    rdfStream.concat(getTriples(LdpContainerRdfContext.class));
                }

                if (ldpPreferences.prefersContainment()) {

                    final Iterator<FedoraResource> children = resource().getChildren();

                    rdfStream.concat(concat(transform(children,
                            new Function<FedoraResource, RdfStream>() {

                                @Override
                                public RdfStream apply(final FedoraResource child) {
                                    return child.getTriples(translator(), PropertiesRdfContext.class);
                                }
                            })));

                }

                rdfStream.concat(getTriples(ContainerRdfContext.class));
            }
            returnPreference.addResponseHeaders(servletResponse);


            addResponseInformationToStream(resource(), rdfStream, uriInfo,
                    translator());
        }

        return Response.ok(rdfStream).build();
    }

    /**
     * Get the binary content of a datastream
     *
     * @return Binary blob
     * @throws RepositoryException
     */
    protected Response getBinaryContent(final String rangeValue)
            throws IOException {
            final FedoraBinary binary = (FedoraBinary)resource();

            // we include an explicit etag, because the default behavior is to use the JCR node's etag, not
            // the jcr:content node digest. The etag is only included if we are not within a transaction.
            final String txId = TransactionServiceImpl.getCurrentTransactionId(session());
            if (txId == null) {
                checkCacheControlHeaders(request, servletResponse, binary, session());
            }
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            Response.ResponseBuilder builder;

            if (rangeValue != null && rangeValue.startsWith("bytes")) {

                final Range range = Range.convert(rangeValue);

                final long contentSize = binary.getContentSize();

                final String endAsString;

                if (range.end() == -1) {
                    endAsString = Long.toString(contentSize - 1);
                } else {
                    endAsString = Long.toString(range.end());
                }

                final String contentRangeValue =
                        String.format("bytes %s-%s/%s", range.start(),
                                endAsString, contentSize);

                if (range.end() > contentSize ||
                        (range.end() == -1 && range.start() > contentSize)) {

                    builder = status(REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header("Content-Range", contentRangeValue);
                } else {
                    final long maxBufferSize = MAX_BUFFER_SIZE; // 10MB max buffer size?
                    final long rangeStart = range.start();
                    final long rangeSize = range.size() == -1 ? contentSize - rangeStart : range.size();
                    final long remainingBytes = contentSize - rangeStart;
                    final long bufSize = rangeSize < remainingBytes ? rangeSize : remainingBytes;

                    if (bufSize < maxBufferSize) {
                        // Small size range content retrieval use javax.jcr.Binary to improve performance
                        final byte[] buf = new byte[(int) bufSize];

                        final Binary binaryContent = binary.getBinaryContent();
                        try {
                            binaryContent.read(buf, rangeStart);
                        } catch (RepositoryException e1) {
                            throw new RepositoryRuntimeException(e1);
                        }
                        binaryContent.dispose();

                        builder = status(PARTIAL_CONTENT).entity(buf)
                                .header("Content-Range", contentRangeValue);
                    } else {
                        // For large range content retrieval, go with the InputStream class to balance
                        // the memory usage, though this is a rare case in range content retrieval.
                        final InputStream content = binary.getContent();
                        final RangeRequestInputStream rangeInputStream =
                                new RangeRequestInputStream(content, range.start(), range.size());

                        builder = status(PARTIAL_CONTENT).entity(rangeInputStream)
                                .header("Content-Range", contentRangeValue);
                    }
                }

            } else {
                final InputStream content = binary.getContent();
                builder = ok(content);
            }

            final ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                    .fileName(binary.getFilename())
                    .creationDate(binary.getCreatedDate())
                    .modificationDate(binary.getLastModifiedDate())
                    .size(binary.getContentSize())
                    .build();

            addResourceHttpHeaders(binary);
            return builder.type(binary.getMimeType())
                    .header("Accept-Ranges", "bytes")
                    .header("Content-Disposition", contentDisposition)
                    .cacheControl(cc)
                    .build();

        }

    protected RdfStream getTriples(final Class<? extends RdfStream> x) {
        return getTriples(resource(), x);
    }

    protected RdfStream getTriples(final FedoraResource resource, final Class<? extends RdfStream> x) {
        return resource.getTriples(translator(), x);
    }

    protected URI getUri(final FedoraResource resource) {
        try {
            final String uri = translator().getSubject(resource.getPath()).getURI();

            if (resource instanceof Datastream) {
                return new URI(uri + "/fcr:metadata");
            } else {
                return new URI(uri);
            }
        } catch (final URISyntaxException e) {
            throw new BadRequestException(e);
        }
    }

    abstract String path();
    abstract List<PathSegment> pathList();

    protected FedoraResource resource() {
        if (resource == null) {
            resource = getResourceFromPath();
        }

        return resource;
    }

    protected FedoraResource getResourceFromPath() {
        final FedoraResource resource;
        try {
            final boolean metadata = pathList().get(pathList().size() - 1).getPath().equals("fcr:metadata");

            final Node node = session().getNode(path());

            if (DatastreamImpl.hasMixin(node)) {
                final DatastreamImpl datastream = new DatastreamImpl(node);

                if (metadata) {
                    resource = datastream;
                } else {
                    resource = datastream.getBinary();
                }
            } else if (FedoraBinaryImpl.hasMixin(node)) {
                resource = new FedoraBinaryImpl(node);
            } else {
                resource = new FedoraObjectImpl(node);
            }
            return resource;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected HttpIdentifierTranslator translator() {
        if (identifierTranslator == null) {
            identifierTranslator = new VersionAwareHttpIdentifierTranslator(session(), FedoraLdp.class, uriInfo);
        }

        return identifierTranslator;
    }

}
