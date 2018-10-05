/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.rdf;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.fcrepo.integration.http.api.AbstractResourceIT;

import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.apache.jena.graph.NodeFactory.createBlankNode;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author cabeer
 * @author ajs6f
 */
public abstract class AbstractIntegrationRdfIT extends AbstractResourceIT {

    protected HttpResponse createLDPRSAndCheckResponse(final String pid, final String body) {
        return createLDPRSAndCheckResponse(pid, body, null);
    }

    protected HttpResponse createLDPRSAndCheckResponse(final String pid, final String body,
            final Map<String, String> headers) {
        try {
            final HttpPut httpPut = new HttpPut(serverAddress + pid);
            if (headers != null && !headers.isEmpty()) {
                headers.keySet().stream().forEach(k -> httpPut.addHeader(k, headers.get(k)));
            }
            if (httpPut.getFirstHeader(CONTENT_TYPE) == null) {
                httpPut.addHeader(CONTENT_TYPE, "text/turtle");
            }
            httpPut.setHeader("Slug", pid);
            final BasicHttpEntity e = new BasicHttpEntity();
            e.setContent(IOUtils.toInputStream(body, UTF_8));
            httpPut.setEntity(e);
            final HttpResponse response = client.execute(httpPut);
            checkResponse(response, CREATED);

            final String location = response.getFirstHeader("Location").getValue();

            final HttpGet httpGet = new HttpGet(location);
            httpGet.addHeader("Prefer", "return=representation; " +
                    "include=\"http://www.w3.org/ns/ldp#PreferMinimalContainer\"; " +
                    "omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");
            final Dataset dataset = getDataset(httpGet);

            final DatasetGraph graphStore = dataset.asDatasetGraph();
            assertFalse(graphStore.isEmpty());

            final Graph tidiedGraph = getTidiedGraph(graphStore);
            final Model expected = ModelFactory.createDefaultModel().read(
                    IOUtils.toInputStream(body, UTF_8), location, "TTL");

            final boolean isomorphicWith = tidiedGraph.isIsomorphicWith(getTidiedGraph(expected.getGraph()));

            final String description;

            if (!isomorphicWith) {
                final ByteArrayOutputStream o = new ByteArrayOutputStream();

                final Model tidiedModel = ModelFactory.createModelForGraph(tidiedGraph);
                tidiedModel.setNsPrefixes(expected.getNsPrefixMap());
                o.write("Expected: ".getBytes());
                RDFDataMgr.write(o, expected, RDFLanguages.TTL);
                o.write("to be isomorphic with: ".getBytes());
                RDFDataMgr.write(o, tidiedModel, RDFLanguages.TTL);
                description = IOUtils.toString(o.toByteArray(), "UTF-8");
            } else {
                description = "";
            }


            assertTrue(description, isomorphicWith);

            return response;
        } catch (final IOException e) {
            assertTrue("Got IOException " + e, false);
            return null;
        }
    }

    private static Graph getTidiedGraph(final DatasetGraph graph) {
        return getTidiedGraph(graph.getDefaultGraph());
    }

    private static Graph getTidiedGraph(final Graph graph) {
        final Graph betterGraph = GraphFactory.createDefaultGraph();
        final ExtendedIterator<Triple> triples = graph.find(Node.ANY, Node.ANY, Node.ANY);
        final Map<Node, Node> bnodeMap = new HashMap<>();

        while (triples.hasNext()) {
            final Triple next = triples.next();

            Triple replacement = next;

            if (isSkolemizedBnode(replacement.getSubject())) {
                if (!bnodeMap.containsKey(replacement.getSubject())) {
                    bnodeMap.put(replacement.getSubject(), createBlankNode());
                }

                replacement = new Triple(bnodeMap.get(replacement.getSubject()),
                        replacement.getPredicate(),
                        replacement.getObject());
            }

            if (isSkolemizedBnode(replacement.getObject())) {

                if (!bnodeMap.containsKey(replacement.getObject())) {
                    bnodeMap.put(replacement.getObject(), createBlankNode());
                }

                replacement = new Triple(replacement.getSubject(),
                        replacement.getPredicate(),
                        bnodeMap.get(replacement.getObject()));
            }

            if (replacement.getObject().isLiteral()
                    && replacement.getObject().getLiteral().getDatatype() != null
                    && replacement.getObject().getLiteral().getDatatype().equals(XSDDatatype.XSDstring)) {
                replacement = new Triple(replacement.getSubject(),
                        replacement.getPredicate(),
                        createLiteral(replacement.getObject().getLiteral().getLexicalForm()));
            }

            betterGraph.add(replacement);
        }
        return betterGraph;
    }

    private static boolean isSkolemizedBnode(final Node node) {
        if (!node.isURI()) {
            return false;
        }
        final URI uri = URI.create(node.toString());
        return uri.getFragment() != null && uri.getFragment().startsWith("genid");
    }

    protected void checkResponse(final HttpResponse response, final Response.StatusType expected) {
        final int actual = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response!", expected.getStatusCode(), actual);
    }

    protected String getContentFromClasspath(final String path) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(path), UTF_8);
    }


}
