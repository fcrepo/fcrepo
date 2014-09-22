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
package org.fcrepo.integration.http.api;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createModelForGraph;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.TimeZone.getTimeZone;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static nu.validator.htmlparser.common.DoctypeExpectation.NO_DOCTYPE_ERRORS;
import static nu.validator.htmlparser.common.XmlViolationPolicy.ALLOW;
import static org.apache.http.impl.client.cache.CacheConfig.DEFAULT;
import static org.apache.jena.riot.WebContent.contentTypeN3;
import static org.apache.jena.riot.WebContent.contentTypeN3Alt1;
import static org.apache.jena.riot.WebContent.contentTypeN3Alt2;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.RdfLexicon.DC_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.FIRST_PAGE;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_SIZE;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_IDENTIFIER;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.RdfLexicon.JCR_NT_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.NEXT_PAGE;
import static org.fcrepo.kernel.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.RESTAPI_NAMESPACE;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.ws.rs.core.Variant;

import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.saxtree.TreeBuilder;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

/**
 * <p>FedoraNodesIT class.</p>
 *
 * @author awoods
 */
public class FedoraNodesIT extends AbstractResourceIT {

    private static final String TEST_ACTIVATION_PROPERTY = "RUN_TEST_CREATE_MANY";
    private SimpleDateFormat headerFormat;
    private SimpleDateFormat tripleFormat;

    @Before
    public void setup() {
        headerFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        headerFormat.setTimeZone(getTimeZone("GMT"));
        tripleFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        tripleFormat.setTimeZone(getTimeZone("GMT"));
    }

    @Test
    public void testIngest() throws Exception {
        final String pid = getRandomUniquePid();

        final HttpResponse response = createObject(pid);

        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));
        assertTrue("Didn't find Location header!", response.containsHeader("Location"));
    }

    @Test
    public void testIngestWithNewAndSparqlQuery() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(
                ("INSERT { <> <http://purl.org/dc/elements/1.1/title> \"this is a title\" } WHERE {}")
                .getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);
        final String location = response.getFirstHeader("Location").getValue();

        final HttpGet httpGet = new HttpGet(location);

        final GraphStore graphStore = getGraphStore(httpGet);

        assertTrue(graphStore.contains(ANY, createResource(location).asNode(),
                DC_TITLE.asNode(), createPlainLiteral("this is a title")
                        .asNode()));

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        final String lastmod = response.getFirstHeader("Last-Modified").getValue();
        assertNotNull("Should set Last-Modified for new nodes", lastmod);
        assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
    }

    @Test
    public void testIngestWithNewAndGraph() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <http://purl.org/dc/elements/1.1/title> \"this is a title\".";
        entity.setContent(new ByteArrayInputStream(rdf.getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);

        final String location = response.getFirstHeader("Location").getValue();

        final HttpGet httpGet = new HttpGet(location);

        final GraphStore graphStore = getGraphStore(httpGet);

        assertTrue(graphStore.contains(ANY, createResource(location).asNode(),
                DC_TITLE.asNode(), createPlainLiteral("this is a title")
                        .asNode()));

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        final String lastmod = response.getFirstHeader("Last-Modified").getValue();
        assertNotNull("Should set Last-Modified for new nodes", lastmod);
        assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
    }

    @Test
    public void testIngestWithSlug() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Slug", getRandomUniquePid());
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        final String location = response.getFirstHeader("Location").getValue();
        assertNotEquals(serverAddress + "/objects", location);

        assertEquals("Object wasn't created!", OK.getStatusCode(),
                getStatus(new HttpGet(location)));
    }

    @Test
    public void testIngestWithRepeatedSlug() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpPut put = new HttpPut(serverAddress + pid);
        assertEquals(201, getStatus(put));

        final HttpPost method = postObjMethod("");
        method.addHeader("Slug", pid);
        assertEquals(409, getStatus(method));
    }

    @Test
    public void testIngestWithBinary() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/octet-stream");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream("xyz".getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        final String location = response.getFirstHeader("Location").getValue();
        assertNotEquals(serverAddress + "/objects", location);
        assertTrue(location.endsWith("fcr:content"));
        assertEquals("Object wasn't created!", OK.getStatusCode(),
                getStatus(new HttpGet(location)));
    }

    @Test
    public void testDeleteObject() throws Exception {
        final HttpResponse response = createObject("");
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(204, getStatus(new HttpDelete(location)));
        assertEquals("Object wasn't really deleted!", 404,
                getStatus(new HttpGet(location)));
    }

    @Test
    public void testCreateManyObjects() throws Exception {
        if (System.getProperty(TEST_ACTIVATION_PROPERTY) == null) {
            logger.info("Not running test because system property not set: {}", TEST_ACTIVATION_PROPERTY);
            return;
        }

        final int manyObjects = 2000;
        for ( int i = 0; i < manyObjects; i++ ) {
            Thread.sleep(10); // needed to prevent overloading
            final HttpResponse response = createObject("");
            logger.debug( response.getFirstHeader("Location").getValue() );
        }
    }

    @Test
    public void testDeleteWithBadEtag() throws Exception {

        final HttpPost method = postObjMethod("");
        final HttpResponse response = client.execute(method);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine()
                                                  .getStatusCode());

        final String location = response.getFirstHeader("Location").getValue();
        final HttpDelete request = new HttpDelete(location);
        request.addHeader("If-Match", "\"doesnt-match\"");
        final HttpResponse deleteResponse = client.execute(request);
        assertEquals(412, deleteResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testGetDatastream() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpResponse response =
            execute(new HttpGet(serverAddress + pid + "/ds1"));
        assertEquals(EntityUtils.toString(response.getEntity()), 200, response
                .getStatusLine().getStatusCode());
        assertEquals(TURTLE, response.getFirstHeader("Content-Type").getValue());

        final Collection<String> links =
            map(response.getHeaders("Link"), new Function<Header, String>() {

                @Override
                public String apply(final Header h) {
                    return h.getValue();
                }
            });
        assertTrue("Didn't find 'describes' link header!",
                      links.contains("<" + serverAddress + pid + "/ds1/fcr:content>;rel=\"describes\""));

    }

    @Test
    public void testGetDatastreamContentWithJCRBadBath() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpResponse response =
            execute(new HttpGet(serverAddress + pid + "/ds1/jcr:content"));
        assertEquals(NOT_FOUND.getStatusCode(), response
                .getStatusLine().getStatusCode());
    }

    @Test
    public void testRangeRequestWithJCRContentBadPath() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/jcr:content");
        method_test_get.setHeader("Range", "bytes=1-3");
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(method_test_get));
    }

    @Test
    public void testPutDatastreamContentWithJCRContentBadPath() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpPut method_test_put = new HttpPut(serverAddress + pid + "/ds1/jcr:content");
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(method_test_put));

    }

    @Test
    public void testDeleteDatastream() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpDelete dmethod =
            new HttpDelete(serverAddress + pid + "/ds1");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get =
            new HttpGet(serverAddress +  pid + "/ds1");
        assertEquals(404, getStatus(method_test_get));
    }

    @Test
    public void testDeleteDatastreamContentWithJCRBadPath() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpDelete dmethod =
            new HttpDelete(serverAddress + pid + "/ds1/jcr:content");
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(dmethod));
    }


    @Test
    public void testHeadRepositoryGraph() throws Exception {
        final HttpHead headObjMethod = new HttpHead(serverAddress);
        assertEquals(200, getStatus(headObjMethod));
    }

    @Test
    public void testGetRepositoryGraph() throws Exception {
        final HttpGet getObjMethod = new HttpGet(serverAddress);
        final GraphStore graphStore = getGraphStore(getObjMethod);
        logger.trace("Retrieved repository graph:\n" + graphStore.toString());

        assertTrue("expected to find the root node data", graphStore.contains(
                ANY, ANY, HAS_PRIMARY_TYPE.asNode(), createLiteral(ROOT)));

    }

    @Test
    public void testGetObjectGraphHtml() throws Exception {
        final HttpResponse createResponse = createObject("");

        final String location = createResponse.getFirstHeader("Location").getValue();

        final HttpGet getObjMethod = new HttpGet(location);
        getObjMethod.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.trace("Retrieved: {}", content);
    }

    @Test
    public void testGetObjectGraphVariants() throws Exception {
        final HttpResponse createResponse = createObject("");

        final String location = createResponse.getFirstHeader("Location").getValue();

        for (final Variant variant : RDFMediaType.POSSIBLE_RDF_VARIANTS) {

            final HttpGet getObjMethod =
                    new HttpGet(location);

            final String type = variant.getMediaType().getType();

            getObjMethod.addHeader("Accept", type);
            final HttpResponse response = client.execute(getObjMethod);

            final int expected = OK.getStatusCode();
            final int found = response.getStatusLine().getStatusCode();

            assertEquals("Expected: " + expected + ", recieved: " + found + ", " + type, expected, found);
        }
    }

    @Test
    public void testGetObjectGraph() throws Exception {
        logger.debug("Entering testGetObjectGraph()...");
        final HttpResponse createResponse = createObject("");

        final String location = createResponse.getFirstHeader("Location").getValue();

        final HttpGet getObjMethod =
                new HttpGet(location);
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        testOptionsHeaders(response);

        final Collection<String> links =
            map(response.getHeaders("Link"), new Function<Header, String>() {

                @Override
                public String apply(final Header h) {
                    return h.getValue();
                }
            });
        assertTrue("Didn't find LDP link header!", links
                .contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));
        final GraphStore results = getGraphStore(getObjMethod);
        final Model model = createModelForGraph(results.getDefaultGraph());

        final Resource nodeUri = createResource(location);

        assertTrue("Didn't find an expected triple!", model.contains(nodeUri,
                createProperty(REPOSITORY_NAMESPACE + "mixinTypes"),
                createPlainLiteral("fedora:object")));

        logger.debug("Leaving testGetObjectGraph()...");
    }

    @Test
    public void verifyFullSetOfRdfTypes() throws Exception {
        logger.debug("Entering verifyFullSetOfRdfTypes()...");
        final String pid = getRandomUniquePid();
        createObject(pid);
        addMixin( pid, MIX_NAMESPACE + "versionable" );

        final HttpGet getObjMethod =
                new HttpGet(serverAddress + pid);
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final GraphStore results = getGraphStore(getObjMethod);
        final Model model = createModelForGraph(results.getDefaultGraph());
        final Resource nodeUri = createResource(serverAddress + pid);
        final Property rdfType = createProperty(RDF_NAMESPACE + "type");

        //verifyResource based on the expection of these types on an out of the box fedora object:
        /*
                http://fedora.info/definitions/v4/rest-api#object
                http://fedora.info/definitions/v4/rest-api#relations
                http://fedora.info/definitions/v4/rest-api#resource
                http://purl.org/dc/elements/1.1/describable
                http://www.jcp.org/jcr/mix/1.0created
                http://www.jcp.org/jcr/mix/1.0lastModified
                http://www.jcp.org/jcr/mix/1.0lockable
                http://www.jcp.org/jcr/mix/1.0referenceable
                http://www.jcp.org/jcr/mix/1.0simpleVersionable
                http://www.jcp.org/jcr/mix/1.0versionable
                http://www.jcp.org/jcr/nt/1.0base
                http://www.jcp.org/jcr/nt/1.0folder
                http://www.jcp.org/jcr/nt/1.0hierarchyNode
                http://www.w3.org/ns/ldp#Container
                http://www.w3.org/ns/ldp#DirectContainer
                http://www.w3.org/ns/ldp#Page
        */

        verifyResource(model, nodeUri, rdfType, RESTAPI_NAMESPACE, "object");
        verifyResource(model, nodeUri, rdfType, RESTAPI_NAMESPACE, "relations");
        verifyResource(model, nodeUri, rdfType, RESTAPI_NAMESPACE, "resource");
        verifyResource(model, nodeUri, rdfType, LDP_NAMESPACE, "DirectContainer");
        verifyResource(model, nodeUri, rdfType, DC_NAMESPACE, "describable");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "created");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "lastModified");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "lockable");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "referenceable");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "simpleVersionable");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "versionable");
        verifyResource(model, nodeUri, rdfType, JCR_NT_NAMESPACE, "base");
        verifyResource(model, nodeUri, rdfType, JCR_NT_NAMESPACE, "folder");
        verifyResource(model, nodeUri, rdfType, JCR_NT_NAMESPACE, "hierarchyNode");

        logger.debug("Leaving verifyFullSetOfRdfTypes()...");
    }


    @Test
    public void testGetObjectGraphWithChildren() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpResponse createResponse = createObject(pid);
        final String location = createResponse.getFirstHeader("Location").getValue();

        createObject(pid + "/a");
        createObject(pid + "/b");
        createObject(pid + "/c");
        final HttpGet getObjMethod = new HttpGet(serverAddress + pid);
        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                                             .getStatusCode());
        final Model model = createDefaultModel();
        model.read(response.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.trace(
                    "Retrieved object graph:\n {}",
                    w);
        }

        final Resource subjectUri = createResource(location);
        assertTrue(
                "Didn't find child node!",
                model.contains(
                        subjectUri,
                createProperty(REPOSITORY_NAMESPACE + "hasChild"),
                createResource(location + "/c")));
        final Collection<String> links =
            map(response.getHeaders("Link"), new Function<Header, String>() {

                @Override
                public String apply(final Header h) {
                    return h.getValue();
                }
            });
        assertTrue("Didn't find LDP resource link header!",
                   links.contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));
        assertTrue("Didn't find LDP container link header!",
                   links.contains("<" + LDP_NAMESPACE + "DirectContainer>;rel=\"type\""));
    }

    @Test
    public void testGetObjectGraphMinimal() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        createObject(pid + "/a");
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + pid);
        getObjMethod.addHeader("Prefer", "return=minimal");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                                             .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.trace("Retrieved object graph:\n" + content);

        assertFalse(
                "Didn't expect member resources",
                compile(
                        "<"
                                + serverAddress
                                + pid + "> <" + HAS_CHILD + ">",
                        DOTALL).matcher(content).find());

        assertFalse("Didn't expect contained member resources",
                       compile(
                                  "<"
                                      + serverAddress
                                      + pid + "> <" + CONTAINS + ">",
                                  DOTALL).matcher(content).find());
    }

    @Test
    public void testGetObjectOmitMembership() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        createObject(pid + "/a");
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + pid);
        getObjMethod.addHeader("Prefer",
                               "return=representation; " +
                                       "omit=\"http://www.w3.org/ns/ldp#PreferContainment " +
                                       "http://www.w3.org/ns/ldp#PreferMembership\"");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                                             .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.trace("Retrieved object graph:\n" + content);

        assertFalse(
                       "Didn't expect inlined member resources",
                       compile(
                                  "<"
                                      + serverAddress
                                      + pid + "> <" + HAS_CHILD + ">",
                                  DOTALL).matcher(content).find());

    }

    @Test
    public void testGetObjectOmitContainment() throws Exception {
        final String pid = "testGetObjectOmitContainment-" + getRandomUniquePid();
        createObject(pid);
        createObject(pid + "/a");
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + pid);
        getObjMethod.addHeader("Prefer", "return=representation; omit=\"http://www.w3.org/ns/ldp#PreferContainment\"");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                                             .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.trace("Retrieved object graph:\n" + content);

        assertTrue("Didn't find member resources",
                compile("<" + serverAddress + pid + "> <" + HAS_CHILD + ">",
                        DOTALL).matcher(content).find());

        assertFalse("Didn't expect contained member resources",
                       compile(
                                  "^<"
                                      + serverAddress
                                      + pid + "/a>",
                                  DOTALL).matcher(content).find());

    }

    @Test
    public void testGetObjectReferences() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        createObject(pid + "/a");
        createObject(pid + "/b");

        final HttpPatch updateObjectGraphMethod = new HttpPatch(serverAddress + pid + "/a");

        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");

        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(
            new ByteArrayInputStream(
                ("INSERT { " +
                 "<" + serverAddress + pid + "/a" + "> <http://fedora.info/definitions/v4/rels-ext#isPartOf> <"
                        + serverAddress + pid + "/b" + "> . \n" +
                 "<" + serverAddress + pid + "/a" + "> <info:xyz#some-other-property> <" + serverAddress + pid + "/b"
                        + "> " + "} WHERE {}").getBytes()));

        updateObjectGraphMethod.setEntity(e);
        client.execute(updateObjectGraphMethod);

        final HttpGet getObjMethod =  new HttpGet(serverAddress + pid + "/b");

        getObjMethod.addHeader("Prefer", "return=representation; include=\"" + INBOUND_REFERENCES.toString() + "\"");
        getObjMethod.addHeader("Accept", "application/n-triples");

        final GraphStore graphStore = getGraphStore(getObjMethod);

        assertTrue(graphStore.contains(Node.ANY,
                                          NodeFactory.createURI(serverAddress + pid + "/a"),
                                          NodeFactory.createURI("http://fedora.info/definitions/v4/rels-ext#isPartOf"),
                                          NodeFactory.createURI(serverAddress + pid + "/b")
                                          ));

        assertTrue(graphStore.contains(Node.ANY,
                                          NodeFactory.createURI(serverAddress + pid + "/a"),
                                          NodeFactory.createURI("info:xyz#some-other-property"),
                                          NodeFactory.createURI(serverAddress + pid + "/b")
        ));

    }

    @Test
    public void testGetObjectGraphByUUID() throws Exception {
        final HttpResponse createResponse = createObject("");

        final String location = createResponse.getFirstHeader("Location").getValue();

        final HttpGet getObjMethod = new HttpGet(location);
        final GraphStore graphStore = getGraphStore(getObjMethod);
        final Iterator<Quad> iterator =
            graphStore.find(ANY, createURI(location),
                        HAS_PRIMARY_IDENTIFIER.asNode(), ANY);

        assertTrue("Expected graph to contain a UUID", iterator.hasNext());

        final String uuid = iterator.next().getObject().getLiteralLexicalForm();

        final HttpGet getObjMethodByUuid =
                new HttpGet(serverAddress + "%5B" + uuid + "%5D");
        getObjMethodByUuid.addHeader("Accept", "application/n3");
        final HttpResponse uuidResponse = client.execute(getObjMethod);
        assertEquals(200, uuidResponse.getStatusLine().getStatusCode());

    }

    @Test
    public void testLinkToNonExistent() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch patch = new HttpPatch(subjectURI);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
            ("INSERT { <> <http://fedora.info/definitions/v4/rels-ext#isMemberOfCollection> " +
                 "<" + serverAddress + "non-existant> } WHERE {}").getBytes()));
        patch.setEntity(e);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testEmtpyPatch() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch patch = new HttpPatch(subjectURI);
        patch.addHeader("Content-Type", "application/sparql-update");
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testUpdateObjectGraph() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch updateObjectGraphMethod =
            new HttpPatch(subjectURI);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + subjectURI +
                        "> <http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\" } WHERE {}")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());

    }

    @Test
    public void testUpdateAndReplaceObjectGraph() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);

        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");

        BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + subjectURI + "> <info:rubydora#label> \"asdfg\" } WHERE {}")
                        .getBytes()));

        updateObjectGraphMethod.setEntity(e);
        client.execute(updateObjectGraphMethod);

        e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(("DELETE { <" + subjectURI
                + "> <info:rubydora#label> ?p}\n" + "INSERT {<" + subjectURI
                + "> <info:rubydora#label> \"qwerty\"} \n" + "WHERE { <"
                + subjectURI + "> <info:rubydora#label> ?p}").getBytes()));

        updateObjectGraphMethod.setEntity(e);

        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));

        final HttpGet getObjMethod = new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(getResponse.getEntity());
        logger.trace("Retrieved object graph:\n" + content);

        assertFalse("Found a triple we thought we deleted.", compile(
                "<" + subjectURI + "> <info:rubydora#label> \"asdfg\" \\.",
                DOTALL).matcher(content).find());

    }

    @Test
    public void testUpdateObjectGraphWithProblems() throws Exception {

        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();

        final HttpPatch patchObjMethod = new HttpPatch(subjectURI);
        patchObjMethod.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + subjectURI + "> <" + REPOSITORY_NAMESPACE +
                        "uuid> \"00e686e2-24d4-40c2-92ce-577c0165b158\" } WHERE {}\n")
                        .getBytes()));
        patchObjMethod.setEntity(e);
        final HttpResponse response = client.execute(patchObjMethod);

        if (response.getStatusLine().getStatusCode() != 403
                && response.getEntity() != null) {
            final String content = EntityUtils.toString(response.getEntity());
            logger.trace("Got unexpected update response:\n" + content);
        }
        assertEquals(403, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testRepeatedPut() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpPut firstPut = new HttpPut(serverAddress + pid);
        assertEquals(201, getStatus(firstPut));

        final HttpPut secondPut = new HttpPut(serverAddress + pid);
        assertEquals(409, getStatus(secondPut));
    }

    @Test
    public void testFilteredLDPTypes() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPut put = new HttpPut(serverAddress + pid);
        put.addHeader("Content-Type", "text/rdf+n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
               "<> a <http://www.w3.org/ns/ldp#IndirectContainer>".getBytes()));
        put.setEntity(e);
        assertEquals(204, getStatus(put));
    }

    @Test
    public void testReplaceGraph() throws Exception {
        final HttpResponse object = createObject("");
        final String subjectURI =  object.getFirstHeader("Location").getValue();
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("<" + subjectURI + "> <info:rubydora#label> \"asdfg\"")
                        .getBytes()));
        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(204, response.getStatusLine().getStatusCode());
        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));


        final HttpGet getObjMethod = new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.trace(
                    "Retrieved object graph for testReplaceGraph():\n {}", w);
        }
        assertTrue("Didn't find a triple we tried to create!", model.contains(
                createResource(subjectURI),
                createProperty("info:rubydora#label"),
                createPlainLiteral("asdfg")));
    }

    @Test
    public void testCreateGraph() throws Exception {
        final String subjectURI = serverAddress + getRandomUniquePid();
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("<" + subjectURI + "> <info:rubydora#label> \"asdfg\"")
                        .getBytes()));
        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(201, response.getStatusLine().getStatusCode());


        final HttpGet getObjMethod = new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine()
                .getStatusCode());
        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.trace("Retrieved object graph for testCreateGraph():\n {}",
                    w);
        }
        assertTrue("Didn't find a triple we tried to create!", model.contains(
                createResource(subjectURI),
                createProperty("info:rubydora#label"),
                createPlainLiteral("asdfg")));
    }

    @Test
    public void testRoundTripReplaceGraph() throws Exception {

        final String pid = getRandomUniquePid();
        final String subjectURI = serverAddress + pid;

        createObject(pid);

        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader("Accept", "text/turtle");
        getObjMethod.addHeader("Prefer", "return=minimal");
        final HttpResponse getResponse = client.execute(getObjMethod);

        final BasicHttpEntity e = new BasicHttpEntity();

        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), subjectURI, "TURTLE");

        try (final StringWriter w = new StringWriter()) {
            model.write(w, "TURTLE");
            e.setContent(new ByteArrayInputStream(w.toString().getBytes()));
            logger.trace("Retrieved object graph for testRoundTripReplaceGraph():\n {}",
                            w);
        }

        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "text/turtle");

        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(204, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testRoundTripReplaceGraphForDatastream() throws Exception {

        final String pid = getRandomUniquePid();
        final String subjectURI = serverAddress + pid + "/ds1";

        createDatastream(pid, "ds1", "some-content");

        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader("Accept", "text/turtle");
        getObjMethod.addHeader("Prefer", "return=minimal");
        final HttpResponse getResponse = client.execute(getObjMethod);

        final BasicHttpEntity e = new BasicHttpEntity();

        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), subjectURI, "TURTLE");

        try (final StringWriter w = new StringWriter()) {
            model.write(w, "TURTLE");
            e.setContent(new ByteArrayInputStream(w.toString().getBytes()));
            logger.trace("Retrieved object graph for testRoundTripReplaceGraphForDatastream():\n {}",
                            w);
        }

        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "text/turtle");

        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(204, response.getStatusLine().getStatusCode());

    }
    @Test
    public void testGetGraphForDatastreamWithJCRBadPath() throws Exception {

        final String pid = getRandomUniquePid();
        final String subjectURI = serverAddress + pid + "/ds1/jcr:content";

        createDatastream(pid, "ds1", "some-content");

        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader("Accept", "text/turtle");
        getObjMethod.addHeader("Prefer", "return=minimal");
        final HttpResponse response = client.execute(getObjMethod);

        assertEquals(NOT_FOUND.getStatusCode(), response.getStatusLine().getStatusCode());

    }

    @Ignore("pending https://www.pivotaltracker.com/story/show/78647248")
    @Test
    public void testDescribeSize() throws Exception {

        final String sizeNode = getRandomUniquePid();

        GraphStore graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.trace("For testDescribeSize() first size retrieved repository graph:\n"
                + graphStore.toString());

        Iterator<Triple> iterator =
            graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_SIZE.asNode(),
                    ANY);

        final String oldSize = (String) iterator.next().getObject().getLiteralValue();

        createObject(sizeNode);
        createDatastream(sizeNode, "asdf", "1234");

        graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.trace("For testDescribeSize() new size retrieved repository graph:\n"
                + graphStore.toString());

        iterator =
            graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_SIZE.asNode(),
                    ANY);

        final String newSize = (String) iterator.next().getObject().getLiteralValue();

        logger.trace("Old size was: " + oldSize + " and new size was: "
                + newSize);
        assertTrue("No increment in size occurred when we expected one!",
                Integer.parseInt(oldSize) < Integer.parseInt(newSize));
    }

    @Ignore("pending https://www.pivotaltracker.com/story/show/78647248")
    @Test
    public void testDescribeCount() throws Exception {
        logger.trace("Entering testDescribeCount()...");
        GraphStore graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.trace("For testDescribeCount() first count retrieved repository graph:\n"
                + graphStore.toString());

        Iterator<Triple> iterator =
            graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_COUNT.asNode(),
                    ANY);

        final String oldSize = (String) iterator.next().getObject().getLiteralValue();

        createObject("");
        final String countNode = getRandomUniquePid();
        createDatastream(countNode, "asdf", "1234");

        graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.trace("For testDescribeCount() first count repository graph:\n"
                + graphStore.toString());

        iterator =
            graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_COUNT.asNode(),
                    ANY);

        final String newSize =
                 (String) iterator.next().getObject().getLiteralValue();

        logger.debug("Old size was: " + oldSize + " and new size was: " +
                             newSize);
        assertTrue("No increment in count occurred when we expected one!",
                   Integer.parseInt(oldSize) < Integer.parseInt(newSize));
    }

    /**
     * Given a directory at: test-FileSystem1/ /ds1 /ds2 /TestSubdir/
     * and a projection of test-objects as fedora:/files, then I should be able
     * to retrieve an object from fedora:/files/FileSystem1 that lists a child
     * object at fedora:/files/FileSystem1/TestSubdir and lists datastreams ds1
     * and ds2
     */
    @Test
    public void testGetProjectedNode() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "files/FileSystem1");
        final Graph result = getGraphStore(method).getDefaultGraph();

        final String subjectURI = serverAddress + "files/FileSystem1";
        logger.trace("For testGetProjectedNode() retrieved graph:\n"
                + result.toString());
        assertTrue("Didn't find the first datastream! ", result.contains(
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds1")));
        assertTrue("Didn't find the second datastream! ", result.contains(
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds2")));
        assertTrue("Didn't find the first object! ", result.contains(
                createURI(subjectURI), ANY, createURI(subjectURI
                        + "/TestSubdir")));

    }

    @Test
    public void testDescribeRdfCached() throws IOException {
        try (final CloseableHttpClient cachingClient =
                CachingHttpClientBuilder.create().setCacheConfig(DEFAULT).build()) {

            final HttpResponse createResponse = createObject("");
            final String location = createResponse.getFirstHeader("Location").getValue();
            final HttpGet getObjMethod = new HttpGet(location);

            HttpResponse response = cachingClient.execute(getObjMethod);
            assertEquals("Client didn't return a OK!", OK.getStatusCode(), response
                    .getStatusLine().getStatusCode());
            logger.debug("Found HTTP headers:\n{}", Joiner.on('\n').join(
                    response.getAllHeaders()));
            assertTrue("Didn't find Last-Modified header!", response
                    .containsHeader("Last-Modified"));
            final String lastModed =
                    response.getFirstHeader("Last-Modified").getValue();
            final String etag = response.getFirstHeader("ETag").getValue();
            final HttpGet getObjMethod2 = new HttpGet(location);
            getObjMethod2.setHeader("If-Modified-Since", lastModed);
            getObjMethod2.setHeader("If-None-Match", etag);
            response = cachingClient.execute(getObjMethod2);

            assertEquals("Client didn't return a NOT_MODIFIED!", NOT_MODIFIED
                    .getStatusCode(), response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testValidHTMLForRepo() throws Exception {
        validateHTML("");
    }

    @Test
    public void testValidHTMLForObject() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        validateHTML(pid);
    }

    @Test
    public void testValidHTMLForDS() throws Exception {
        final String pid = getRandomUniquePid();
        client.execute(new HttpPut(serverAddress
                + pid + "/ds/fcr:content"));
        validateHTML(pid + "/ds");
    }

    @Test
    public void testGetHTMLForDSWithJCRBadPath() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpResponse response = client.execute(new HttpPut(serverAddress
                + pid + "/ds/jcr:content"));
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    @Test
    public void testCopy() throws Exception {
        final HttpResponse response  = createObject("");
        final String pid = getRandomUniquePid();
        final String location = response.getFirstHeader("Location").getValue();
        final HttpCopy request = new HttpCopy(location);
        request.addHeader("Destination", serverAddress + pid);
        client.execute(request);

        final HttpGet httpGet = new HttpGet(serverAddress + pid);

        final HttpResponse copiedResult = client.execute(httpGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        final HttpResponse originalResult = client.execute(new HttpGet(location));
        assertEquals(OK.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    @Test
    public void testCopyDestExists() throws Exception {

        final HttpResponse response1 = createObject("");
        final String location1 = response1.getFirstHeader("Location").getValue();
        final HttpResponse response2 = createObject("");
        final String location2 = response2.getFirstHeader("Location").getValue();

        final HttpCopy request = new HttpCopy(location1);
        request.addHeader("Destination", location2);
        final HttpResponse result = client.execute(request);

        assertEquals(PRECONDITION_FAILED.getStatusCode(), result.getStatusLine().getStatusCode());
    }

    @Test
    public void testCopyInvalidDest() throws Exception {

        final HttpResponse response1 = createObject("");
        final String location1 = response1.getFirstHeader("Location").getValue();

        final HttpCopy request = new HttpCopy(location1);
        request.addHeader("Destination", serverAddress + "non/existent/path");
        assertEquals(CONFLICT.getStatusCode(), getStatus(request));
    }

    @Test
    public void testMove() throws Exception {

        final String pid = getRandomUniquePid();
        final HttpResponse response = createObject("");
        final String location = response.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location);
        request.addHeader("Destination", serverAddress + pid);
        client.execute(request);

        final HttpGet httpGet = new HttpGet(serverAddress + pid);

        final HttpResponse copiedResult = client.execute(httpGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        final HttpResponse originalResult = client.execute(new HttpGet(location));
        assertEquals(NOT_FOUND.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    @Test
    public void testMoveDestExists() throws Exception {

        final HttpResponse response1 = createObject("");
        final String location1 = response1.getFirstHeader("Location").getValue();
        final HttpResponse response2 = createObject("");
        final String location2 = response2.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location1);
        request.addHeader("Destination", location2);
        final HttpResponse result = client.execute(request);

        assertEquals(PRECONDITION_FAILED.getStatusCode(), result.getStatusLine().getStatusCode());
    }

    @Test
    public void testMoveInvalidDest() throws Exception {

        final HttpResponse response1 = createObject("");
        final String location1 = response1.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location1);
        request.addHeader("Destination", serverAddress + "non/existent/destination");
        assertEquals(CONFLICT.getStatusCode(), getStatus(request));
    }

    @Test
    public void testMoveWithBadEtag() throws Exception {

        final String pid = getRandomUniquePid();
        final HttpResponse response = createObject("");
        final String location = response.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location);
        request.addHeader("Destination", serverAddress + pid);
        request.addHeader("If-Match", "\"doesnt-match\"");
        final HttpResponse moveResponse = client.execute(request);
        assertEquals(412, moveResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testOptions() throws Exception {
        final HttpResponse response = createObject("");
        final String location = response.getFirstHeader("Location").getValue();
        final HttpOptions optionsRequest = new HttpOptions(location);
        final HttpResponse optionsResponse = client.execute(optionsRequest);
        assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());

        testOptionsHeaders(optionsResponse);
    }

    private void testOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse,"Allow");
        assertTrue("Should allow GET", methods.contains(HttpGet.METHOD_NAME));
        assertTrue("Should allow POST", methods.contains(HttpPost.METHOD_NAME));
        assertTrue("Should allow PUT", methods.contains(HttpPut.METHOD_NAME));
        assertTrue("Should allow PATCH", methods.contains(HttpPatch.METHOD_NAME));
        assertTrue("Should allow DELETE", methods.contains(HttpDelete.METHOD_NAME));
        assertTrue("Should allow OPTIONS", methods.contains(HttpOptions.METHOD_NAME));
        assertTrue("Should allow MOVE", methods.contains(HttpMove.METHOD_NAME));
        assertTrue("Should allow COPY", methods.contains(HttpCopy.METHOD_NAME));

        final List<String> patchTypes = headerValues(httpResponse,"Accept-Patch");
        assertTrue("PATCH should support application/sparql-update", patchTypes.contains(contentTypeSPARQLUpdate));

        final List<String> postTypes = headerValues(httpResponse,"Accept-Post");
        assertTrue("POST should support application/sparql-update", postTypes.contains(contentTypeSPARQLUpdate));
        assertTrue("POST should support text/turtle", postTypes.contains(contentTypeTurtle));
        assertTrue("POST should support text/rdf+n3", postTypes.contains(contentTypeN3));
        assertTrue("POST should support application/n3", postTypes.contains(contentTypeN3Alt1));
        assertTrue("POST should support text/n3", postTypes.contains(contentTypeN3Alt2));
        assertTrue("POST should support application/rdf+xml", postTypes.contains(contentTypeRDFXML));
        assertTrue("POST should support application/n-triples", postTypes.contains(contentTypeNTriples));
        assertTrue("POST should support multipart/form-data", postTypes.contains("multipart/form-data"));
    }

    private static List<String> headerValues( final HttpResponse response, final String headerName ) {
        final List<String> values = new ArrayList<>();
        for (final Header header : response.getHeaders(headerName)) {
            for (final String elem : header.getValue().split(",")) {
                values.add(elem.trim());
            }
        }
        return values;
    }

    @Test
    public void testResponseContentTypes() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        for (final String type : RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method =
                    new HttpGet(serverAddress + pid);

            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
     }

    private void validateHTML(final String path) throws Exception {
        final HttpGet getMethod = new HttpGet(serverAddress + path);
        getMethod.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(getMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.trace("Retrieved HTML view:\n" + content);

        final HtmlParser htmlParser = new HtmlParser(ALLOW);
        htmlParser.setDoctypeExpectation(NO_DOCTYPE_ERRORS);
        htmlParser.setErrorHandler(new HTMLErrorHandler());
        htmlParser.setContentHandler(new TreeBuilder());
        try (
            final InputStream htmlStream =
                new ByteArrayInputStream(content.getBytes())) {
            htmlParser.parse(new InputSource(htmlStream));
        }
        logger.debug("HTML found to be valid.");
    }

    private static void verifyResource(final Model model,
                                final Resource nodeUri,
                                final Property rdfType,
                                final String namespace,
                                final String resource) {
        assertTrue("Didn't find rdfType " + namespace + resource,
                   model.contains(nodeUri, rdfType, createResource(namespace + resource)));
    }

    public static class HTMLErrorHandler implements ErrorHandler {

        @Override
        public void warning(final SAXParseException e) {
            fail(e.toString());
        }

        @Override
        public void error(final SAXParseException e) {
            fail(e.toString());
        }

        @Override
        public void fatalError(final SAXParseException e) {
            fail(e.toString());
        }
    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpCopy extends HttpRequestBase {

        public final static String METHOD_NAME = "COPY";


        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpCopy(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpMove extends HttpRequestBase {

        public final static String METHOD_NAME = "MOVE";


        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpMove(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }

    /**
     * I should be able to upload a file to a read/write federated filesystem.
    **/
    @Test
    public void testUploadToProjection() throws IOException {
        // upload file to federated filesystem using rest api
        final String pid = getRandomUniquePid();
        final String uploadLocation = serverAddress + "files/" + pid + "/ds1/fcr:content";
        final String uploadContent = "abc123";
        logger.debug("Uploading to federated filesystem via rest api: " + uploadLocation);
        final HttpResponse response = createDatastream("files/" + pid, "ds1", uploadContent);
        final String actualLocation = response.getFirstHeader("Location").getValue();
        assertEquals("Wrong URI in Location header", uploadLocation, actualLocation);

        // validate content
        final HttpGet get = new HttpGet(uploadLocation);
        final HttpResponse getResponse = client.execute(get);
        final String actualContent = EntityUtils.toString( getResponse.getEntity() );
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
        assertEquals("Content doesn't match", actualContent, uploadContent);

        // validate object profile
        final HttpGet objGet = new HttpGet(serverAddress + "files/" + pid);
        final HttpResponse objResponse = client.execute(objGet);
        assertEquals(OK.getStatusCode(), objResponse.getStatusLine().getStatusCode());
    }

    /**
     * I should be able to copy objects from the repository to a federated filesystem.
    **/
    @Test
    public void testCopyToProjection() throws IOException {
        // create object in the repository
        final String pid = getRandomUniquePid();
        createDatastream(pid, "ds1", "abc123");

        // copy to federated filesystem
        final HttpCopy request = new HttpCopy(serverAddress + pid);
        request.addHeader("Destination", serverAddress + "files/copy-" + pid);
        final HttpResponse copyResponse = client.execute(request);
        assertEquals(CREATED.getStatusCode(), copyResponse.getStatusLine().getStatusCode());

        // federated copy should now exist
        final HttpGet copyGet = new HttpGet(serverAddress + "files/copy-" + pid);
        final HttpResponse copiedResult = client.execute(copyGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        // repository copy should still exist
        final HttpGet originalGet = new HttpGet(serverAddress + pid);
        final HttpResponse originalResult = client.execute(originalGet);
        assertEquals(OK.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    /**
     * I should be able to copy objects from a federated filesystem to the repository.
    **/
    @Test
    public void testCopyFromProjection() throws IOException {
        final String destination = serverAddress + "copy-" + getRandomUniquePid() + "-ds1";
        final String source = serverAddress + "files/FileSystem1/ds1";

        // ensure the source is present
        final HttpGet get = new HttpGet(source);
        final HttpResponse getResponse = client.execute(get);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());

        // copy to repository
        final HttpCopy request = new HttpCopy(source);
        request.addHeader("Destination", destination);
        final HttpResponse copyRequest = client.execute(request);
        assertEquals(CREATED.getStatusCode(), copyRequest.getStatusLine().getStatusCode());

        // repository copy should now exist
        final HttpGet copyGet = new HttpGet(destination);
        final HttpResponse copiedResult = client.execute(copyGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        // federated filesystem copy should still exist
        final HttpGet originalGet = new HttpGet(source);
        final HttpResponse originalResult = client.execute(originalGet);
        assertEquals(OK.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    /**
     * I should be able to link to content on a federated filesystem.
    **/
    @Test
    public void testFederatedDatastream() throws IOException {
        final String federationAddress = serverAddress + "files/FileSystem1/ds1/fcr:content";
        final String repoObj = getRandomUniquePid();
        final String linkingAddress = serverAddress + repoObj;

        // create an object in the repository
        final HttpPut put = new HttpPut(linkingAddress);
        assertEquals(201, getStatus(put));

        // link from the object to the content of the file on the federated filesystem
        final String sparql = "insert data { <> <http://fedora.info/definitions/v4/rels-ext#hasExternalContent> "
                 + "<" + federationAddress + "> . }";
        final HttpPatch patch = new HttpPatch(serverAddress + repoObj);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(sparql.getBytes()));
        patch.setEntity(e);
        assertEquals("Couldn't link to external datastream!", 204, getStatus(patch));
    }

    /**
     * I should be able to move a node within a federated filesystem with
     * properties preserved.
    **/
    @Test
    public void testFederatedMoveWithProperties() throws Exception {
        // create object on federation
        final String pid = getRandomUniquePid();
        final String source = serverAddress + "files/" + pid + "/src";
        createObject("files/" + pid + "/src");

        // add properties
        final HttpPatch patch = new HttpPatch(source);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        final String sparql = "insert { <> <http://purl.org/dc/elements/1.1/identifier> \"identifier.123\" . "
            + "<> <http://purl.org/dc/elements/1.1/title> \"title.123\" } where {}";
        e.setContent(new ByteArrayInputStream(sparql.getBytes()));
        patch.setEntity(e);
        final HttpResponse response = client.execute(patch);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());

        // move object
        final String destination = serverAddress + "files/" + pid + "/dst";
        final HttpMove request = new HttpMove(source);
        request.addHeader("Destination", destination);
        final HttpResponse moveRequest = client.execute(request);
        assertEquals(CREATED.getStatusCode(), moveRequest.getStatusLine().getStatusCode());

        // check properties
        final HttpGet get =  new HttpGet(destination);
        get.addHeader("Accept", "application/n-triples");
        final GraphStore graphStore = getGraphStore(get);
        assertTrue(graphStore.contains(Node.ANY, NodeFactory.createURI(destination),
                                                 NodeFactory.createURI("http://purl.org/dc/elements/1.1/identifier"),
                                                 NodeFactory.createLiteral("identifier.123")));
        assertTrue(graphStore.contains(Node.ANY, NodeFactory.createURI(destination),
                                                 NodeFactory.createURI("http://purl.org/dc/elements/1.1/title"),
                                                 NodeFactory.createLiteral("title.123")));
    }

    @Test
    @Ignore("https://www.pivotaltracker.com/story/show/59240160")
    public void testPaging() throws Exception {
        // create a node with 4 children
        final String pid = getRandomUniquePid();
        final Node parent = createResource(serverAddress + pid).asNode();
        createObject(pid);
        createObject(pid + "/child1");
        createObject(pid + "/child2");
        createObject(pid + "/child3");
        createObject(pid + "/child4");

        // get first page
        final HttpGet firstGet = new HttpGet(serverAddress + pid + "?limit=2");
        final HttpResponse firstResponse = execute(firstGet);
        final GraphStore firstGraph = getGraphStore(firstResponse);

        // count children in response graph
        int firstChildCount = 0;
        Iterator<Quad> it = firstGraph.find(ANY,parent,HAS_CHILD.asNode(),ANY);
        for ( ; it.hasNext(); firstChildCount++ ) {
            logger.debug( "Found child: {}", it.next() );
        }
        assertEquals("Should have two children!", 2, firstChildCount);


        // count children in response graph
        int firstContainsCount = 0;
        it = firstGraph.find(ANY,parent,CONTAINS.asNode(),ANY);
        for ( ; it.hasNext(); firstContainsCount++ ) {
            logger.debug( "Found child: {}", it.next() );
        }
        assertEquals("Should have two children!", 2, firstContainsCount);

        // collect link headers
        final Collection<String> firstLinks =
            map(firstResponse.getHeaders("Link"), new Function<Header, String>() {

                @Override
                public String apply(final Header h) {
                    return h.getValue();
                }
            });

        // it should have a first page link
        assertTrue("Didn't find first page header!",firstLinks.contains("<" + serverAddress + pid
                + "?limit=2&amp;offset=0>;rel=\"first\""));
        assertTrue("Didn't find first page triple!", firstGraph.contains(ANY, ANY, FIRST_PAGE.asNode(),
                createResource(serverAddress + pid + "?limit=2&amp;offset=0").asNode()));

        // it should have a next page link
        assertTrue("Didn't find next page header!", firstLinks.contains("<" + serverAddress + pid
                + "?limit=2&amp;offset=2>;rel=\"next\""));
        assertTrue("Didn't find next page triple!", firstGraph.contains(ANY, ANY, NEXT_PAGE.asNode(),
                createResource(serverAddress + pid + "?limit=2&amp;offset=2").asNode()));


        // get second page
        final HttpGet nextGet = new HttpGet(serverAddress + pid + "?limit=2&offset=2");
        final HttpResponse nextResponse = execute(nextGet);
        final GraphStore nextGraph = getGraphStore(nextResponse);

        // it should have two inlined resources
        int nextChildCount = 0;
        for (it = nextGraph.find(ANY,parent,HAS_CHILD.asNode(),ANY); it.hasNext(); nextChildCount++ ) {
            logger.debug( "Found child: {}", it.next() );
        }
        assertEquals("Should have two children!", 2, nextChildCount);

        // collect link headers
        final Collection<String> nextLinks =
            map(nextResponse.getHeaders("Link"), new Function<Header, String>() {

                @Override
                public String apply(final Header h) {
                    return h.getValue();
                }
            });

        // it should have a first page link
        assertTrue("Didn't find first page header!", nextLinks.contains("<" + serverAddress + pid
                + "?limit=2&amp;offset=0>;rel=\"first\""));
        assertTrue("Didn't find first page triple!", nextGraph.contains(ANY, ANY, FIRST_PAGE.asNode(),
                createResource(serverAddress + pid + "?limit=2&amp;offset=0").asNode()));

        // it should not have a next page link
        for ( final String link : nextLinks ) {
            assertFalse("Should not have next page header!", link.contains("rel=\"next\""));
        }
        assertFalse("Should not have next pagiple!", nextGraph.contains(ANY, ANY, NEXT_PAGE.asNode(), ANY));
    }

    @Test
    public void testLinkedDeletion() throws Exception {
        final String linkedFrom = UUID.randomUUID().toString();
        final String linkedTo = UUID.randomUUID().toString();
        createObject(linkedFrom);
        createObject(linkedTo);

        final String sparql = "insert data { <" + serverAddress + linkedFrom + "> "
                 + "<http://fedora.info/definitions/v4/rels-ext#isMemberOfCollection> "
                 + "<" + serverAddress + linkedTo + "> . }";
        final HttpPatch patch = new HttpPatch(serverAddress + linkedFrom);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(sparql.getBytes()));
        patch.setEntity(e);
        assertEquals("Couldn't link resources!", 204, getStatus(patch));

        final HttpDelete delete = new HttpDelete(serverAddress + linkedTo);
        assertEquals("Error deleting linked-to!", 204, getStatus(delete));

        final HttpGet get = new HttpGet(serverAddress + linkedFrom);
        assertEquals("Linked to should still exist!", 200, getStatus(get));
    }

    /**
     * When I make changes to a resource in a federated filesystem, the parent
     * folder's Last-Modified header should be updated.
    **/
    @Test
    public void testLastModifiedUpdatedAfterUpdates() throws Exception {

        // create directory containing a file in filesystem
        final File fed = new File("target/test-classes/test-objects");
        final String id = getRandomUniquePid();
        final File dir = new File( fed, id );
        final File child = new File( dir, "child" );
        final long timestamp1 = System.currentTimeMillis();
        dir.mkdir();
        child.mkdir();
        Thread.sleep(2000);

        // check Last-Modified header is current
        final HttpHead head1 = new HttpHead(serverAddress + "files/" + id);
        final HttpResponse resp1 = client.execute(head1);
        assertEquals( 200, resp1.getStatusLine().getStatusCode() );
        final long lastmod1 = headerFormat.parse(resp1.getFirstHeader("Last-Modified").getValue()).getTime();
        assertTrue( (timestamp1 - lastmod1) < 1000 ); // because rounding

        // remove the file and wait for the TTL to expire
        final long timestamp2 = System.currentTimeMillis();
        child.delete();
        Thread.sleep(2000);

        // check Last-Modified header is updated
        final HttpHead head2 = new HttpHead(serverAddress + "files/" + id);
        final HttpResponse resp2 = client.execute(head2);
        assertEquals( 200, resp2.getStatusLine().getStatusCode() );
        final long lastmod2 = headerFormat.parse(resp2.getFirstHeader("Last-Modified").getValue()).getTime();
        assertTrue( (timestamp2 - lastmod2) < 1000 ); // because rounding

        assertFalse("Last-Modified headers should have changed", lastmod1 == lastmod2);
    }

    @Test
    public void testUpdateObjectWithSpaces() throws Exception {
        final String id = getRandomUniquePid() + " 2";
        final HttpResponse createResponse = createObject(id);
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
            "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test\" } WHERE {}".getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    @Test
    public void testCreatedAndModifiedDates() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String location = createResponse.getFirstHeader("Location").getValue();
        final HttpGet getObjMethod = new HttpGet(location);
        final HttpResponse response = client.execute(getObjMethod);
        final GraphStore results = getGraphStore(response);
        final Model model = createModelForGraph(results.getDefaultGraph());
        final Resource nodeUri = createResource(location);

        final String lastmodString = response.getFirstHeader("Last-Modified").getValue();
        headerFormat.parse(lastmodString);
        final Date createdDateTriples = getDateFromModel( model, nodeUri,
                createProperty(REPOSITORY_NAMESPACE + "created"));
        final Date lastmodDateTriples = getDateFromModel( model, nodeUri,
                createProperty(REPOSITORY_NAMESPACE + "lastModified"));
        assertNotNull( createdDateTriples );
        assertEquals( lastmodString, headerFormat.format(createdDateTriples) );
        assertNotNull( lastmodDateTriples );
        assertEquals( lastmodString, headerFormat.format(lastmodDateTriples) );
    }

    private Date getDateFromModel( final Model model, final Resource subj, final Property pred ) throws Exception {
        final StmtIterator stmts = model.listStatements( subj, pred, (String)null );
        if ( stmts.hasNext() ) {
            return tripleFormat.parse(stmts.nextStatement().getString());
        }
        return null;
    }

    /**
     * I should be able to create two subdirectories of a non-existent parent
     * directory.
    **/
    @Test
    public void testBreakFederation() throws Exception {
        testGetRepositoryGraph();
        createObject("files/a0/b0");
        createObject("files/a0/b1");
        testGetRepositoryGraph();
    }

}
