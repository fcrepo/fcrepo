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

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.impl.services.TransactionServiceImpl;
import org.glassfish.jersey.media.multipart.ContentDisposition;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;

import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.fcrepo.kernel.RdfLexicon.NON_RDF_SOURCE;

/**
 * An abstract class that sits between AbstractResource and any resource that
 * wishes to share the routines for building responses containing binary
 * content.
 *
 * @author Mike Durbin
 */
public abstract class ContentExposingResource extends AbstractResource {

    protected static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;

    protected static final int PARTIAL_CONTENT = 206;

    private static long MAX_BUFFER_SIZE = 10240000;

    /**
     * A helper method that does most of the work associated with processing a request
     * for content (or a range of the content) into a Response
     */
    protected Response getDatastreamContentResponse(final FedoraBinary binary,
                                                    final String rangeValue,
                                                    final Request request,
                                                    final HttpServletResponse servletResponse,
                                                    final HttpIdentifierTranslator subjects,
                                                    final Session session) throws
            RepositoryException, IOException {

        // we include an explicit etag, because the default behavior is to use the JCR node's etag, not
        // the jcr:content node digest. The etag is only included if we are not within a transaction.
        final String txId = TransactionServiceImpl.getCurrentTransactionId(session);
        if (txId == null) {
            checkCacheControlHeaders(request, servletResponse, binary, session);
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
                    binaryContent.read(buf, rangeStart);
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

        return builder.type(binary.getMimeType())
                .header("Link", "<" + subjects.getSubject(binary.getDescription().getPath()) + ">;rel=\"describedby\"")
                .header("Link", "<" + NON_RDF_SOURCE + ">;rel=\"type\"")
                .header("Accept-Ranges", "bytes")
                .header("Content-Disposition", contentDisposition)
                .cacheControl(cc)
                .build();
    }

    /**
     * Evaluate the cache control headers for the request to see if it can be served from
     * the cache.
     *
     * @param request
     * @param servletResponse
     * @param resource
     * @param session
     * @throws javax.jcr.RepositoryException
     */
    protected static void checkCacheControlHeaders(final Request request,
                                                   final HttpServletResponse servletResponse,
                                                   final FedoraBinary resource,
                                                   final Session session) throws RepositoryException {

        final EntityTag etag = new EntityTag(resource.getContentDigest().toString());
        final Date date = resource.getLastModifiedDate();

        final Date roundedDate = new Date();
        if (date != null) {
            roundedDate.setTime(date.getTime() - date.getTime() % 1000);
        }
        final Response.ResponseBuilder builder =
            request.evaluatePreconditions(roundedDate, etag);

        if (builder != null) {
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            // here we are implicitly emitting a 304
            // the exception is not an error, it's genuinely
            // an exceptional condition
            throw new WebApplicationException(builder.cacheControl(cc)
                                                  .lastModified(date).tag(etag).build());
        }

        addCacheControlHeaders(servletResponse, resource, session);
    }

    /**
     * Add ETag and Last-Modified cache control headers to the response
     * @param servletResponse
     * @param resource
     * @throws RepositoryException
     */
    protected static void addCacheControlHeaders(final HttpServletResponse servletResponse,
                                                 final FedoraBinary resource,
                                                 final Session session) {

        // Do not add caching headers if in a transaction
        final String txId = TransactionServiceImpl.getCurrentTransactionId(session);
        if (txId != null) {
            return;
        }

        final Date date = resource.getLastModifiedDate();

        final URI contentDigest = resource.getContentDigest();
        if (contentDigest != null) {
            final EntityTag etag = new EntityTag(contentDigest.toString());
            if (!etag.getValue().isEmpty()) {
                servletResponse.addHeader("ETag", etag.toString());
            }
        }

        if (date != null) {
            servletResponse.addDateHeader("Last-Modified", date.getTime());
        }
    }

    /**
     * Setter that set the max buffer size for range content retrieval,
     * which could help for unit testing.
     * @param maxBufferSize
     */
    public static void setMaxBufferSize(final long maxBufferSize) {
        MAX_BUFFER_SIZE = maxBufferSize;
    }
}
