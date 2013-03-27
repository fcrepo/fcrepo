package org.fcrepo.api;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.sitemap.SitemapEntry;
import org.fcrepo.jaxb.responses.sitemap.SitemapIndex;
import org.fcrepo.jaxb.responses.sitemap.SitemapUrlSet;
import org.fcrepo.jaxb.search.FieldSearchResult;
import org.fcrepo.provider.VelocityViewer;
import org.fcrepo.services.ObjectService;
import org.fcrepo.services.RepositoryService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.Source;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static java.lang.Integer.parseInt;
import static javax.jcr.query.Query.JCR_SQL2;
import static org.fcrepo.utils.FedoraJcrTypes.*;
import static org.slf4j.LoggerFactory.getLogger;

@Path("/sitemap")
public class FedoraSitemap extends AbstractResource {

    private static final Logger logger = getLogger(FedoraSitemap.class);

    @Inject
    ObjectService objectService;

    @GET
    public SitemapIndex getSitemapIndex() throws RepositoryException {
        final Session session = repo.login();
        try {
            final long count = objectService.getRepositoryObjectCount(session) / 50000;

            SitemapIndex sitemapIndex = new SitemapIndex();

            for(int i = 0; i <= count; i++ ) {
                sitemapIndex.appendSitemapEntry(new SitemapEntry(uriInfo.getBaseUriBuilder().path(FedoraSitemap.class).path(FedoraSitemap.class, "getSitemap").build(i+1)));
            }

            return sitemapIndex;
        } finally {
            session.logout();
        }
    }

    @GET
    @Path("/{page}")
    public SitemapUrlSet getSitemap(@PathParam("page") final String page) throws RepositoryException {
        final Session session = repo.login();
        try {
            final SitemapUrlSet sitemapUrlSet = new SitemapUrlSet();

            RowIterator rows = getSitemapEntries(session, parseInt(page));

            while(rows.hasNext()) {
                Row r = rows.nextRow();

                sitemapUrlSet.appendSitemapEntry(getSitemapEntry(r));
            }

            return sitemapUrlSet;
        } finally {
            session.logout();
        }
    }

    private RowIterator getSitemapEntries(Session session, int page) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();

        //TODO expand to more fields
        String sqlExpression = "SELECT [jcr:name],[jcr:lastModified] FROM [" + FEDORA_OBJECT + "]";
        Query query = queryManager.createQuery(sqlExpression, JCR_SQL2);

        query.setOffset(page*50000);
        query.setLimit(50000);


        QueryResult queryResults = query.execute();

        final RowIterator rows = queryResults.getRows();

        return rows;
    }

    private SitemapEntry getSitemapEntry(Row r) throws RepositoryException {
        return new SitemapEntry(uriInfo.getBaseUriBuilder().path(FedoraObjects.class).path(FedoraObjects.class, "getObject").build(r.getNode().getName()), r.getValue("jcr:lastModified").getDate());
    }


}
