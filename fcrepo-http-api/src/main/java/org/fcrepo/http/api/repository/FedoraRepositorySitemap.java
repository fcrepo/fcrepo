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
package org.fcrepo.http.api.repository;

import static java.lang.Integer.parseInt;
import static javax.jcr.query.Query.JCR_SQL2;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_CREATED;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.modeshape.jcr.api.JcrConstants.JCR_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.jaxb.responses.sitemap.SitemapEntry;
import org.fcrepo.http.commons.jaxb.responses.sitemap.SitemapIndex;
import org.fcrepo.http.commons.jaxb.responses.sitemap.SitemapUrlSet;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;

/**
 * A Sitemap implementation for Fedora objects
 *
 * TODO should this be fcr:sitemap?
 * @author ajs6f
 * @author cbeer
 */
@Scope("prototype")
@Path("/sitemap")
public class FedoraRepositorySitemap extends AbstractResource {

    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraRepositorySitemap.class);

    public static final long entriesPerPage = 50000;

    /**
     * Get the sitemap index for the repository GET /sitemap
     *
     * @return sitemap index for the repository
     */
    @GET
    @Timed
    @Produces(TEXT_XML)
    public SitemapIndex getSitemapIndex() {

        LOGGER.trace("Executing getSitemapIndex()...");

        final long count =
                repositoryService.getRepositoryObjectCount() / entriesPerPage;

        final SitemapIndex sitemapIndex = new SitemapIndex();

        for (int i = 0; i <= count; i++) {
            sitemapIndex
                    .appendSitemapEntry(new SitemapEntry(uriInfo
                            .getBaseUriBuilder().path(FedoraRepositorySitemap.class)
                            .path(FedoraRepositorySitemap.class, "getSitemap").build(
                                    i + 1)));
        }
        LOGGER.trace("Executed getSitemapIndex().");
        return sitemapIndex;

    }

    /**
     * Get the sitemap at a given page
     *
     * @param page
     * @return sitemap at the given page
     * @throws RepositoryException
     */
    @GET
    @Path("/{page}")
    @Timed
    @Produces(TEXT_XML)
    public SitemapUrlSet getSitemap(@PathParam("page") final String page) {
        final SitemapUrlSet sitemapUrlSet = new SitemapUrlSet();

        final RowIterator rows =
                getSitemapEntries(session, parseInt(page) - 1);

        while (rows.hasNext()) {
            final Row r = rows.nextRow();

            sitemapUrlSet.appendSitemapEntry(getSitemapEntry(r));
        }

        return sitemapUrlSet;
    }

    private RowIterator getSitemapEntries(final Session session, final long pg) {

        try {
            final QueryManager queryManager =
                    session.getWorkspace().getQueryManager();

            // TODO expand to more fields
            final String sqlExpression =
                    "SELECT [" + JCR_NAME + "],[" + JCR_LASTMODIFIED + "] FROM ["
                            + FEDORA_OBJECT + "]";
            final Query query = queryManager.createQuery(sqlExpression, JCR_SQL2);

            query.setOffset(pg * entriesPerPage);
            query.setLimit(entriesPerPage);

            final QueryResult queryResults = query.execute();

            final RowIterator rows = queryResults.getRows();

            return rows;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private SitemapEntry getSitemapEntry(final Row r) {
        try {

            Value lkDateValue = r.getValue(JCR_LASTMODIFIED);
            final String path = r.getNode().getPath();

            if (lkDateValue == null) {
                LOGGER.warn("no value for {} on {}", JCR_LASTMODIFIED, path);
                lkDateValue = r.getValue(JCR_CREATED);
            }
            final Calendar lastKnownDate =
                    (lkDateValue != null) ? lkDateValue.getDate() : null;
            return new SitemapEntry(uriInfo.getBaseUriBuilder().path(
                    FedoraNodes.class).build(path.substring(1)), lastKnownDate);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
