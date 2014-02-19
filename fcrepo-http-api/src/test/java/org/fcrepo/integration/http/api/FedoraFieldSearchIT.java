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

package org.fcrepo.integration.http.api;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static com.hp.hpl.jena.vocabulary.RDF.nil;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_OF_RESULT;
import static org.fcrepo.kernel.RdfLexicon.NEXT_PAGE;
import static org.fcrepo.kernel.RdfLexicon.PAGE_OF;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_HAS_TOTAL_RESULTS;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_ITEMS_PER_PAGE;
import static org.fcrepo.kernel.RdfLexicon.SEARCH_OFFSET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.junit.Ignore;
import org.junit.Test;

import com.hp.hpl.jena.update.GraphStore;

@Ignore("Destined for death.")
public class FedoraFieldSearchIT extends AbstractResourceIT {

    @Test
    public void testSearchHtml() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "fcr:search");
        method.setHeader("Accept", "text/html");

        final HttpResponse resp = execute(method);

        final String content = IOUtils.toString(resp.getEntity().getContent());
        logger.debug("Got search form: {}", content);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertTrue(content.contains("<form"));

    }

    @Test
    public void testSearchResultsHtml() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "fcr:search");
        method.setHeader("Accept", "text/html");
        final URI uri =
            new URIBuilder(method.getURI()).addParameter("q", "testobj")
                    .addParameter("offset", "0").addParameter("limit", "1")
                    .build();

        method.setURI(uri);

        final HttpResponse resp = execute(method);

        final String content = IOUtils.toString(resp.getEntity().getContent());
        logger.debug("Got search results: {}", content);
        assertEquals(200, resp.getStatusLine().getStatusCode());

    }

    @Test
    public void testSearchRdf() throws Exception {
        /* first post an object which can be used for the search */
        createObject("testobj");

        /* and add a dc title to the object so the query returns a result */
        setProperty("testobj", "http://purl.org/dc/elements/1.1/title",
                "testobj");

        final HttpGet method = new HttpGet(serverAddress + "fcr:search");
        final URI uri =
            new URIBuilder(method.getURI()).addParameter("q", "testobj")
                    .addParameter("offset", "0").addParameter("limit", "1")
                    .build();

        method.setURI(uri);

        final GraphStore graphStore =  getGraphStore(method);

        logger.debug("Got search results graph: {}", graphStore);
        assertTrue(graphStore.contains(ANY, createResource(
                serverAddress + "fcr:search?q=testobj").asNode(),
                SEARCH_HAS_TOTAL_RESULTS.asNode(), createTypedLiteral(1)
                        .asNode()));

        assertTrue(graphStore.contains(ANY, createResource(
                serverAddress + "fcr:search?q=testobj&offset=0&limit=1")
                .asNode(), PAGE_OF.asNode(), createResource(
                serverAddress + "fcr:search?q=testobj").asNode()));

        assertTrue(graphStore.contains(ANY, createResource(
                serverAddress + "fcr:search?q=testobj&offset=0&limit=1")
                .asNode(), SEARCH_OFFSET.asNode(), createTypedLiteral(0)
                .asNode()));

        assertTrue(graphStore.contains(ANY, createResource(
                serverAddress + "fcr:search?q=testobj&offset=0&limit=1")
                .asNode(), SEARCH_ITEMS_PER_PAGE.asNode(),
                createTypedLiteral(1).asNode()));

        assertTrue(graphStore.contains(ANY, createResource(
                serverAddress + "fcr:search?q=testobj&offset=0&limit=1")
                .asNode(), NEXT_PAGE.asNode(), nil.asNode()));

    }

    @Test
    public void testSearchSubmitPaging() throws Exception {

        final HttpGet method = new HttpGet(serverAddress + "fcr:search");
        final URI uri =
                new URIBuilder(method.getURI()).addParameter("q", "testobj")
                        .addParameter("offset", "1").addParameter("limit", "1")
                        .build();

        method.setURI(uri);

        final GraphStore graphStore =  getGraphStore(method);

        logger.debug("Got search results graph: {}", graphStore);
        assertFalse(graphStore.contains(ANY, createResource(
                serverAddress + "fcr:search?q=testobj").asNode(),
                HAS_MEMBER_OF_RESULT.asNode(), ANY));

    }
}
