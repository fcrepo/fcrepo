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

import com.sun.jersey.core.header.ContentDisposition;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.kernel.Datastream;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

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
    protected Response getDatastreamContentResponse(final Datastream ds, final String rangeValue, final Request request,
                                                    final HttpServletResponse servletResponse,
                                                    final HttpIdentifierTranslator subjects) throws
            RepositoryException, IOException {

        // we include an explicit etag, because the default behavior is to use the JCR node's etag, not
        // the jcr:content node digest.
        checkCacheControlHeaders(request, servletResponse, ds);

        final CacheControl cc = new CacheControl();
        cc.setMaxAge(0);
        cc.setMustRevalidate(true);
        Response.ResponseBuilder builder;

        if (rangeValue != null && rangeValue.startsWith("bytes")) {

            final Range range = Range.convert(rangeValue);

            final long contentSize = ds.getContentSize();

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

                    final Binary binaryContent = ds.getBinaryContent();
                    binaryContent.read(buf, rangeStart);
                    binaryContent.dispose();

                    builder = status(PARTIAL_CONTENT).entity(buf)
                            .header("Content-Range", contentRangeValue);
                } else {
                    // For large range content retrieval, go with the InputStream class to balance
                    // the memory usage, though this is a rare case in range content retrieval.
                    final InputStream content = ds.getContent();
                    final RangeRequestInputStream rangeInputStream =
                            new RangeRequestInputStream(content, range.start(), range.size());

                        builder = status(PARTIAL_CONTENT).entity(rangeInputStream)
                                .header("Content-Range", contentRangeValue);
                }
            }

        } else {
            final InputStream content = ds.getContent();
            builder = ok(content);
        }

        final ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                .fileName(ds.getFilename())
                .creationDate(ds.getCreatedDate())
                .modificationDate(ds.getLastModifiedDate())
                .size(ds.getContentSize())
                .build();

        return builder.type(ds.getMimeType()).header(
                "Link",
                "<" + subjects.getSubject(ds.getNode().getPath()) +
                        ">;rel=\"describedby\"").header("Accept-Ranges",
                "bytes").cacheControl(cc)
                .header("Content-Disposition", contentDisposition)
                .build();
    }

    /**
     * Evaluate the cache control headers for the request to see if it can be served from
     * the cache.
     *
     * @param request
     * @param servletResponse
     * @param resource
     * @throws javax.jcr.RepositoryException
     */
    protected static void checkCacheControlHeaders(final Request request,
                                                   final HttpServletResponse servletResponse,
                                                   final Datastream resource) throws RepositoryException {

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

        addCacheControlHeaders(servletResponse, resource);
    }

    /**
     * Add ETag and Last-Modified cache control headers to the response
     * @param servletResponse
     * @param resource
     * @throws RepositoryException
     */
    protected static void addCacheControlHeaders(final HttpServletResponse servletResponse,
                                                 final Datastream resource) throws RepositoryException {

        final EntityTag etag = new EntityTag(resource.getContentDigest().toString());
        final Date date = resource.getLastModifiedDate();

        if (!etag.getValue().isEmpty()) {
            servletResponse.addHeader("ETag", etag.toString());
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
