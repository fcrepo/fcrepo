package org.fcrepo.integration.api;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.fcrepo.api.FedoraSitemap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FedoraSitemapIT extends AbstractResourceIT {
    @Test
    public void testGetSitemapIndex() throws Exception {

        getStatus(postObjMethod("new"));
        final HttpGet httpGet = new HttpGet(serverAddress + "sitemap");

        assertEquals(200, getStatus(httpGet));

        logger.info(IOUtils.toString(execute(httpGet).getEntity().getContent()));

    }

    @Test
    public void testGetSitemap() throws Exception {


        getStatus(postObjMethod("test:1"));
        getStatus(postObjMethod("new"));

        final HttpGet httpGet = new HttpGet(serverAddress + "sitemap/1");

        assertEquals(200, getStatus(httpGet));

        logger.info(IOUtils.toString(execute(httpGet).getEntity().getContent()));

    }
}
