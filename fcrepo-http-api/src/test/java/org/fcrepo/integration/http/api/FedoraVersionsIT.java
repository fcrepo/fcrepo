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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.RdfLexicon.VERSIONING_POLICY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FedoraVersionsIT extends AbstractResourceIT {

    @Test
    public void testGetObjectVersionProfile() throws Exception {
        final String pid = "FedoraDatastreamsTest1";

        createObject(pid);
        final HttpGet getVersion =
            new HttpGet(serverAddress + pid + "/fcr:versions");
        getVersion.addHeader("Accept", RDFMediaType.RDF_XML);
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject = createResource(serverAddress + pid);
        assertTrue("Didn't find a version triple!",
                results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));
    }

    @Test
    public void testAddAndRetrieveVersion() throws Exception {
        execute(postObjMethod("FedoraVersioningTest2"));

        logger.info("Setting a title");
        patchLiteralProperty(serverAddress + "FedoraVersioningTest2", "http://purl.org/dc/elements/1.1/title", "First Title");

        GraphStore nodeResults = getContent(serverAddress + "FedoraVersioningTest2");
        assertTrue("Should find original title", nodeResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), NodeFactory.createLiteral("First Title")));

        logger.info("Posting version v0.0.1");
        final HttpPost postVersion =
                postObjMethod("FedoraVersioningTest2/fcr:versions/v0.0.1");
        execute(postVersion);
        assertEquals(204, getStatus(postVersion));

        logger.info("Replacing the title");
        patchLiteralProperty(serverAddress + "FedoraVersioningTest2", "http://purl.org/dc/elements/1.1/title", "Second Title");

        GraphStore versionResults = getContent(serverAddress + "FedoraVersioningTest2/fcr:versions/v0.0.1");
        logger.info("Got version profile:");

        assertTrue("Didn't find a version triple!",
                      versionResults.contains(Node.ANY, Node.ANY, HAS_PRIMARY_TYPE.asNode(), NodeFactory.createLiteral("nt:frozenNode")));

        assertTrue("Should find a title in historic version", versionResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), Node.ANY));
        assertTrue("Should find original title in historic version", versionResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), NodeFactory.createLiteral("First Title")));
        assertFalse("Should not find the updated title in historic version", versionResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), NodeFactory.createLiteral("Second Title")));
    }

    @Test
    public void testCreateUnlabeledVersion() throws Exception {
        logger.info("Creating an object");
        String objId = "anonymousVersionTestObj";
        execute(postObjMethod(objId));

        logger.info("Setting a title");
        patchLiteralProperty(serverAddress + objId, "http://purl.org/dc/elements/1.1/title", "Example Title");

        logger.info("Posting an unlabeled version");
        final HttpPost postVersion =
                postObjMethod(objId + "/fcr:versions");
        execute(postVersion);
        assertEquals(204, getStatus(postVersion));

    }

    @Test
    public void testCreateTwoVersionsWithSameLabel() throws Exception {
        logger.info("creating an object");
        String objId = "duplicateLabelVersionTestObj";
        execute(postObjMethod(objId));

        logger.info("Setting a title");
        patchLiteralProperty(serverAddress + objId, "http://purl.org/dc/elements/1.1/title", "First title");

        logger.info("posting a version with label \"label\"");
        HttpPost postVersion =
                postObjMethod(objId + "/fcr:versions/label");
        execute(postVersion);
        assertEquals(204, getStatus(postVersion));

        logger.info("Resetting the title");
        patchLiteralProperty(serverAddress + objId, "http://purl.org/dc/elements/1.1/title", "Second title");

        logger.info("posting a version with label \"label\"");
        postVersion = postObjMethod(objId + "/fcr:versions/label");
        execute(postVersion);
        assertEquals(204, getStatus(postVersion));

        logger.info("Resetting the title");
        patchLiteralProperty(serverAddress + objId, "http://purl.org/dc/elements/1.1/title", "Third title");

        GraphStore versionResults = getContent(serverAddress + objId + "/fcr:versions/label");
        logger.info("Got version profile:");

        assertTrue("Should find the title from the last version tagged with the label \"label\"",
                versionResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), NodeFactory.createLiteral("Second title")));

    }

    @Test
    public void testGetDatastreamVersionNotFound() throws Exception {

        final String pid = randomUUID().toString();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpGet getVersion =
            new HttpGet(serverAddress
                    + pid + "/ds1/fcr:versions/lastVersion");
        final HttpResponse resp = execute(getVersion);
        assertEquals(404, resp.getStatusLine().getStatusCode());
    }

    public void mutateDatastream(String objName, String dsName,
                                 String contentText) throws IOException {
        final HttpPut mutateDataStreamMethod =
                putDSMethod(objName, dsName, contentText);
        final HttpResponse response = client.execute(mutateDataStreamMethod);
        final int status = response.getStatusLine().getStatusCode();
        if (status != 204) {
            logger.error(EntityUtils.toString(response.getEntity()));
        }
        assertEquals("Couldn't mutate a datastream!", 204, status);

    }

    @Test
    public void isAutoVersionedContentStillAccessible() throws Exception {
        String objName = "fvt3";
        String dsName = "ds";
        String firstVersionText = "foo";
        String secondVersionText = "bar";

        createObject(objName);
        createDatastream(objName, dsName, firstVersionText);

        setAutoVersioning(serverAddress + objName + "/" + dsName);

        mutateDatastream(objName, dsName, secondVersionText);

        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        objName + "/" + dsName + "/fcr:content");
        assertEquals("Datastream didn't accept mutation!", secondVersionText,
                EntityUtils.toString(
                        client.execute(
                                retrieveMutatedDataStreamMethod).getEntity()));

        final HttpGet getVersion =
                new HttpGet(serverAddress + objName + "/" + dsName + "/fcr:versions");
        getVersion.addHeader("Accept", RDFMediaType.RDF_XML);
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + objName + "/" + dsName);
        assertTrue("Didn't find a version triple!",
                results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));

        final List<String> versionsInOrder = getChronologicalyOrderedVersionUrls(results, subject.asNode());
        assertEquals("There must be a root version with two revisions.", 3, versionsInOrder.size());

        final HttpGet retrieveFirstVersion = new HttpGet(versionsInOrder.get(1) + "/fcr:content");
        assertEquals("First version wasn't preserved as expected!",
                firstVersionText,
                EntityUtils.toString(client.execute(
                        retrieveFirstVersion).getEntity()));
    }

    @Test
    public void testAddMixinAutoVersioning() throws IOException {
        postNodeTypeCNDSnippet("[fedora:autoVersioned] mixin\n" +
                "  - fedora:versioningPolicy (STRING) = \"auto-version\" autocreated");

        String objName = "testMixinAutoVersioning";
        String dsName = "datastream";

        createObject(objName);
        addMixin(serverAddress + objName, "http://fedora.info/definitions/v4/rest-api#autoVersioned");

        GraphStore initialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versioning policy",
                initialVersion.contains(Node.ANY, createResource(serverAddress + objName).asNode(), VERSIONING_POLICY.asNode(), NodeFactory.createLiteral("auto-version")));
    }

    @Test
    public void testRepositoryWideAutoVersioning() throws IOException {
        postNodeTypeCNDSnippet("[fedora:autoVersioned] mixin\n" +
                "  - fedora:versioningPolicy (STRING) = \"auto-version\" autocreated");
        postNodeTypeCNDSnippet("[fedora:resource] > fedora:relations, mix:created, mix:lastModified, mix:lockable, mix:versionable, fedora:autoVersioned, dc:describable mixin\n" +
                "  - rdf:type (URI) multiple\n" +
                "  - * (undefined) multiple\n" +
                "  - * (undefined)");

        String objName = "testRepositoryWideAutoVersioning";
        String dsName = "datastream";

        createObject(objName);

        GraphStore initialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versioning policy",
                initialVersion.contains(Node.ANY, createResource(serverAddress + objName).asNode(), VERSIONING_POLICY.asNode(), NodeFactory.createLiteral("auto-version")));

        testDatastreamContentUpdatesCreateNewVersions(objName, dsName);
    }

    private void testDatastreamContentUpdatesCreateNewVersions(String objName, String dsName) throws IOException {
        String firstVersionText = "foo";
        String secondVersionText = "bar";

        createDatastream(objName, dsName, firstVersionText);

        GraphStore dsInitialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versoning policy",
                dsInitialVersion.contains(Node.ANY, createResource(serverAddress + objName+ "/" + dsName).asNode(), VERSIONING_POLICY.asNode(), NodeFactory.createLiteral("auto-version")));

        mutateDatastream(objName, dsName, secondVersionText);

        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        objName + "/" + dsName + "/fcr:content");
        assertEquals("Datastream didn't accept mutation!", secondVersionText,
                EntityUtils.toString(
                        client.execute(
                                retrieveMutatedDataStreamMethod).getEntity()));

        final HttpGet getVersion =
                new HttpGet(serverAddress + objName + "/" + dsName + "/fcr:versions");
        getVersion.addHeader("Accept", RDFMediaType.RDF_XML);
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + objName + "/" + dsName);
        assertTrue("Didn't find a version triple!",
                results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));


        List<String> versionsInOrder = getChronologicalyOrderedVersionUrls(results, subject.asNode());
        assertEquals("There must be a root version with two revisions.", 3, versionsInOrder.size());

        final HttpGet retrieveFirstVersion = new HttpGet(versionsInOrder.get(1) + "/fcr:content");
        assertEquals("First version wasn't preserved as expected!",
                firstVersionText,
                EntityUtils.toString(client.execute(
                        retrieveFirstVersion).getEntity()));

        final HttpGet retrieveSecondVersion = new HttpGet(versionsInOrder.get(2) + "/fcr:content");
        assertEquals("Second version wasn't preserved as expected!",
                secondVersionText,
                EntityUtils.toString(client.execute(
                        retrieveSecondVersion).getEntity()));
    }

    private List<String> getChronologicalyOrderedVersionUrls(final GraphStore graph, final Node subject) {
        final List<String> versionUrls = new ArrayList<String>();
        final Iterator<Quad> versionIt = graph.find(Node.ANY, subject, HAS_VERSION.asNode(), Node.ANY);
        while (versionIt.hasNext()) {
            versionUrls.add(versionIt.next().getObject().getURI());
        }
        Collections.sort(versionUrls, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String d1 = getFirstLiteralIfExists(graph, createResource(o1).asNode(), LAST_MODIFIED_DATE.asNode(), "");
                String d2 = getFirstLiteralIfExists(graph, createResource(o2).asNode(), LAST_MODIFIED_DATE.asNode(), "");
                if (d1.equals(d2)) {
                    d1 += o1;
                    d2 += o2;
                }
                System.out.println("COMPARING: " + d1 + " compare with " + d2);
                return d1.compareTo(d2);
            }
        });
        for (String url : versionUrls) {
            System.out.println(url + " " + getFirstLiteralIfExists(graph, createResource(url).asNode(), LAST_MODIFIED_DATE.asNode(), "(none)"));
        }
        return versionUrls;
    }

    private String getFirstLiteralIfExists(GraphStore graph, Node subject, Node predicate, String defaultValue) {
        Iterator<Quad> quads = graph.find(Node.ANY, subject, predicate, Node.ANY);
        if (quads.hasNext()) {
            return quads.next().getObject().getLiteralValue().toString();
        }
        return defaultValue;
    }

    public void postNodeTypeCNDSnippet(String snippet) throws IOException {
        final HttpPost httpPost = new HttpPost(serverAddress + "/fcr:nodetypes");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(snippet.getBytes()));
        httpPost.setEntity(entity);
        final HttpResponse response = client.execute(httpPost);

        assertEquals(204, response.getStatusLine().getStatusCode());
    }

    private void setAutoVersioning(String url) throws IOException {
        patchLiteralProperty(url,
                "http://fedora.info/definitions/v4/rest-api#versioningPolicy",
                "auto-version");
    }

    private void patchLiteralProperty(String url, String predicate, String literal) throws IOException {
        final HttpPatch updateObjectGraphMethod =
                new HttpPatch(url);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT DATA { <> <" + predicate + "> \"" + literal + "\" } ")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());
    }

    private void addMixin(String url, String mixinUrl) throws IOException {
        final HttpPatch updateObjectGraphMethod =
                new HttpPatch(url);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();

        e.setContent(new ByteArrayInputStream(
                ("INSERT DATA { <> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + mixinUrl + "> . } ")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());
    }

    private GraphStore getContent(String url) throws IOException {
        final HttpGet getVersion = new HttpGet(url);
        getVersion.addHeader("Accept", RDFMediaType.RDF_XML);
        return getGraphStore(getVersion);
    }

}
