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

package org.fcrepo.integration.http.api.ldp;

import static com.google.common.collect.Iterables.all;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.apache.jena.riot.WebContent.contentTypeN3;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFJSON;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.update.GraphStore;

public class LdprIT extends AbstractResourceIT {

    // 4.2.1
    @Test
    public void testGeneralHttpVersion() throws IOException {
        final HttpHead testMethod = new HttpHead(serverAddress + "");
        final HttpResponse response = client.execute(testMethod);
        assertTrue(response.getProtocolVersion().greaterEquals(HTTP_1_1));
    }

    // 4.2.2
    @Test
    public void testProvidesRDFRepresentation() throws IOException {
        final HttpGet testMethod = new HttpGet(serverAddress + "");
        testMethod.addHeader("Accept", "text/turtle");

        final GraphStore graphStore = getGraphStore(testMethod);

        assertTrue(graphStore.contains(ANY, createResource(serverAddress)
   .asNode(), ANY, ANY));

    }

    // 4.2.3
    // 4.2.4
    // 4.2.4.1
    // 4.2.5
    @Test
    public void testShouldHaveAtLeastOneRdfType() throws IOException {
        final HttpGet testMethod = new HttpGet(serverAddress + "");
        testMethod.addHeader("Accept", "text/turtle");

        final GraphStore graphStore = getGraphStore(testMethod);

        assertTrue(graphStore.contains(ANY, createResource(serverAddress)
                .asNode(), type.asNode(), ANY));

    }

    // 4.2.6
    @Test
    public void testMaySupportStandardRepresentations() {
        final ImmutableList<String> formats =
            ImmutableList.of(contentTypeTurtle, contentTypeNTriples,
                    contentTypeN3, contentTypeRDFJSON, contentTypeRDFXML,
                    TEXT_HTML);

        assertTrue(all(formats, new Predicate<String>() {

            @Override
            public boolean apply(final String input) {
                final HttpGet testMethod = new HttpGet(serverAddress + "");
                testMethod.setHeader("Accept", input);
                try {
                    client.execute(testMethod);
                } catch (final IOException e) {
                    throw new RuntimeException("Failed on format " + input);
                } finally {
                    testMethod.releaseConnection();
                }
                return true;
            }
        }));

    }

    //4.2.7

    // 4.2.8
    @Test
    public void testMustUseEntityTags() throws IOException {
        client.execute(postObjMethod("Ldpr428"));
        final HttpHead testMethod = new HttpHead(serverAddress + "Ldpr428");
        final HttpResponse response = client.execute(testMethod);
        assertTrue(response.containsHeader("ETag"));
    }

    // 4.3.1 (See 4.2.8)
    // TODO 4.3.2

    // 4.3.3 (see 4.2.2)
    // 4.3.4 (see 4.2.2)
    // 4.3.5
    // 4.3.6

    // 4.4

    // 4.5.1
    @Test
    public void testReplaceRoundrip() {

    }

    @Test
    public void testRemoveProperty() {

    }

    @Test
    public void testAddProperty() {

    }

    // 4.5.2
    @Test
    public void testConditionalPut() {

    }

    @Test
    public void testConditionalPutFailure() {

    }

    // 4.5.3
    // 4.5.4
    // 4.5.5

    // TODO 4.5.6


    // 4.6.1
    @Test
    public void testDelete() throws IOException {
        client.execute(postObjMethod("Ldpr461"));

        final HttpGet testMethod = new HttpGet(serverAddress + "Ldpr461");
        final HttpResponse response = client.execute(testMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());

        client.execute(new HttpDelete(serverAddress + "Ldpr461"));

        final HttpResponse postDeleteResponse = client.execute(testMethod);
        assertEquals(404, postDeleteResponse.getStatusLine().getStatusCode());

    }

    // 4.7.1 (see 4.2.8)

    // 4.8.1

    // 4.8.2
    // 4.8.3
    @Test
    public void shouldNotCreateResourcesWithPatch() {

    }

    // 4.8.4
    @Test
    public void shouldIncludeAcceptPatchHeader() {

    }

    // TODO 4.9

}
