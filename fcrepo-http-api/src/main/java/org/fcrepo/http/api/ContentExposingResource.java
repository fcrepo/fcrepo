/**
 * Copyright 2013 DuraSpace, Inc.
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
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.commons.domain.Range;
import org.fcrepo.http.commons.responses.RangeRequestInputStream;
import org.fcrepo.kernel.Datastream;

import javax.jcr.RepositoryException;
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

    /**
     * A helper method that does most of the work associated with processing a request
     * for content (or a range of the content) into a Response
     */
    protected Response getDatastreamContentResponse(final Datastream ds, final String rangeValue, final Request request,
                                                    final HttpGraphSubjects subjects) throws
            RepositoryException, IOException {
        final EntityTag etag =
                new EntityTag(ds.getContentDigest().toString());
        final Date date = ds.getLastModifiedDate();
        final Date roundedDate = new Date();
        roundedDate.setTime(date.getTime() - date.getTime() % 1000);
        Response.ResponseBuilder builder =
                request.evaluatePreconditions(roundedDate, etag);

        final CacheControl cc = new CacheControl();
        cc.setMaxAge(0);
        cc.setMustRevalidate(true);

        if (builder == null) {

            final InputStream content = ds.getContent();

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
                    builder =
                            status(
                                    REQUESTED_RANGE_NOT_SATISFIABLE)
                                    .header("Content-Range",
                                            contentRangeValue);
                } else {
                    final RangeRequestInputStream rangeInputStream =
                            new RangeRequestInputStream(content, range
                                    .start(), range.size());

                    builder =
                            status(PARTIAL_CONTENT).entity(
                                    rangeInputStream)
                                    .header("Content-Range",
                                            contentRangeValue);
                }

            } else {
                builder = ok(content);
            }
        }

        final ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                .fileName(ds.getFilename())
                .creationDate(ds.getCreatedDate())
                .modificationDate(ds.getLastModifiedDate())
                .size(ds.getContentSize())
                .build();

        return builder.type(ds.getMimeType()).header(
                "Link",
                subjects.getGraphSubject(ds.getNode().getPath()) +
                        ";rel=\"meta\"").header("Accept-Ranges",
                "bytes").cacheControl(cc).lastModified(date).tag(etag)
                .header("Content-Disposition", contentDisposition)
                .build();
    }

}
