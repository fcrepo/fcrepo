
package org.fcrepo.api;

import static java.lang.Integer.parseInt;
import static javax.jcr.query.Query.JCR_SQL2;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.yammer.metrics.annotation.Timed;
import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.sitemap.SitemapEntry;
import org.fcrepo.jaxb.responses.sitemap.SitemapIndex;
import org.fcrepo.jaxb.responses.sitemap.SitemapUrlSet;
import org.fcrepo.services.ObjectService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/sitemap")
public class FedoraSitemap extends AbstractResource {

    private static final Logger logger = getLogger(FedoraSitemap.class);

    public static final long entriesPerPage = 50000;

    @Autowired
    private ObjectService objectService;

    @GET
	@Timed
    @Produces(TEXT_XML)
    public SitemapIndex getSitemapIndex() throws RepositoryException {
        logger.trace("Executing getSitemapIndex()...");
        final Session session = getAuthenticatedSession();
        try {
            final long count =
                    objectService.getRepositoryObjectCount(session) /
                            entriesPerPage;

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
        final Session session = getAuthenticatedSession();
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
                "SELECT [jcr:name],[jcr:lastModified] FROM [" + FEDORA_OBJECT +
                        "]";
        final Query query = queryManager.createQuery(sqlExpression, JCR_SQL2);

        query.setOffset(page * entriesPerPage);
        query.setLimit(entriesPerPage);

        final QueryResult queryResults = query.execute();

        final RowIterator rows = queryResults.getRows();

        return rows;
    }

    private SitemapEntry getSitemapEntry(final Row r)
            throws RepositoryException {
        return new SitemapEntry(uriInfo.getBaseUriBuilder().path(
                FedoraObjects.class).path(FedoraObjects.class, "getObject")
                .build(r.getNode().getName()), r.getValue("jcr:lastModified")
                .getDate());
    }

    
    public void setObjectService(ObjectService objectService) {
        this.objectService = objectService;
    }

}
