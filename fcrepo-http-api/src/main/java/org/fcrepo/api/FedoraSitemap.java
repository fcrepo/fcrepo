
package org.fcrepo.api;

import static java.lang.Integer.parseInt;
import static javax.jcr.query.Query.JCR_SQL2;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.utils.FedoraJcrTypes.JCR_CREATED;
import static org.fcrepo.utils.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;

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

import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.sitemap.SitemapEntry;
import org.fcrepo.jaxb.responses.sitemap.SitemapIndex;
import org.fcrepo.jaxb.responses.sitemap.SitemapUrlSet;
import org.fcrepo.session.InjectedSession;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * @author ajs6f
 * @author cbeer
 */
@Component
@Scope("prototype")
@Path("/sitemap")
public class FedoraSitemap extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger logger = getLogger(FedoraSitemap.class);

    public static final long entriesPerPage = 50000;

    @GET
    @Timed
    @Produces(TEXT_XML)
    public SitemapIndex getSitemapIndex() throws RepositoryException {

        logger.trace("Executing getSitemapIndex()...");

        try {
            final long count =
                    objectService.getRepositoryObjectCount() / entriesPerPage;

            final SitemapIndex sitemapIndex = new SitemapIndex();

            for (int i = 0; i <= count; i++) {
                sitemapIndex
                        .appendSitemapEntry(new SitemapEntry(uriInfo
                                .getBaseUriBuilder().path(FedoraSitemap.class)
                                .path(FedoraSitemap.class, "getSitemap").build(
                                        i + 1)));
            }
            logger.trace("Executed getSitemapIndex().");
            return sitemapIndex;
        } finally {
            session.logout();
        }
    }

    @GET
    @Path("/{page}")
    @Timed
    @Produces(TEXT_XML)
    public SitemapUrlSet getSitemap(@PathParam("page")
    final String page) throws RepositoryException {
        try {
            final SitemapUrlSet sitemapUrlSet = new SitemapUrlSet();

            final RowIterator rows =
                    getSitemapEntries(session, parseInt(page) - 1);

            while (rows.hasNext()) {
                final Row r = rows.nextRow();

                sitemapUrlSet.appendSitemapEntry(getSitemapEntry(r));
            }

            return sitemapUrlSet;
        } finally {
            session.logout();
        }
    }

    private RowIterator
            getSitemapEntries(final Session session, final long page)
                    throws RepositoryException {
        final QueryManager queryManager =
                session.getWorkspace().getQueryManager();

        //TODO expand to more fields
        final String sqlExpression =
                "SELECT [" + JcrConstants.JCR_NAME + "],[" + JCR_LASTMODIFIED +
                        "] FROM [" + FEDORA_OBJECT + "]";
        final Query query = queryManager.createQuery(sqlExpression, JCR_SQL2);

        query.setOffset(page * entriesPerPage);
        query.setLimit(entriesPerPage);

        final QueryResult queryResults = query.execute();

        final RowIterator rows = queryResults.getRows();

        return rows;
    }

    private SitemapEntry getSitemapEntry(final Row r)
            throws RepositoryException {
        Value lkDateValue = r.getValue(JCR_LASTMODIFIED);
        final String path = r.getNode().getPath();

        if (lkDateValue == null) {
            logger.warn("no value for {} on {}", JCR_LASTMODIFIED, path);
            lkDateValue = r.getValue(JCR_CREATED);
        }
        final Calendar lastKnownDate =
                (lkDateValue != null) ? lkDateValue.getDate() : null;
        return new SitemapEntry(uriInfo.getBaseUriBuilder().path(
                FedoraNodes.class).build(path.substring(1)), lastKnownDate);
    }

    public void setSession(final Session session) {
        this.session = session;
    }

}
