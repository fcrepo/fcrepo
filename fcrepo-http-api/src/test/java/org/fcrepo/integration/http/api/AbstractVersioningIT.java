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

import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_METADATA;
import static org.fcrepo.kernel.RdfLexicon.EMBED_CONTAINS;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.VERSIONING_POLICY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

/**
 * Framework for running versioning integration tests.
 *
 * @author ajs6f
 */
public abstract class AbstractVersioningIT extends AbstractResourceIT {

    protected void testDatastreamContentUpdatesCreateNewVersions(final String objName, final String dsName)
            throws IOException {
        final String firstVersionText = "foo";
        final String secondVersionText = "bar";
        createDatastream(objName, dsName, firstVersionText);
        final GraphStore dsInitialVersion = getContent(serverAddress + objName + "/" + dsName + "/" + FCR_METADATA);
        assertTrue("Should find auto-created versioning policy",
                dsInitialVersion.contains(ANY,
                        createResource(serverAddress + objName + "/" + dsName + "/" + FCR_METADATA)
                                .asNode(),
                        VERSIONING_POLICY.asNode(),
                        createLiteral("auto-version")));

        mutateDatastream(objName, dsName, secondVersionText);
        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        objName + "/" + dsName);
        assertEquals("Datastream didn't accept mutation!", secondVersionText,
                EntityUtils.toString(
                        execute(
                                retrieveMutatedDataStreamMethod).getEntity()));

        final HttpGet getVersion =
                new HttpGet(serverAddress + objName + "/" + dsName + "/fcr:versions");
        logger.debug("Retrieved version profile:");

        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + objName + "/" + dsName);
        assertTrue("Didn't find a version triple!",
                results.contains(ANY, subject.asNode(), HAS_VERSION.asNode(), ANY));

        verifyVersions(results, subject.asNode(), firstVersionText, secondVersionText);
    }

    protected GraphStore getContent(final String url) throws IOException {
        final HttpGet getVersion = new HttpGet(url);
        getVersion.addHeader("Prefer", "return=representation; include=\"" + EMBED_CONTAINS.toString() + "\"");
        return getGraphStore(getVersion);
    }

    public void mutateDatastream(final String objName, final String dsName, final String contentText)
            throws IOException {
        final HttpPut mutateDataStreamMethod =
                putDSMethod(objName, dsName, contentText);
        final HttpResponse response = execute(mutateDataStreamMethod);
        final int status = response.getStatusLine().getStatusCode();
        if (status != NO_CONTENT.getStatusCode()) {
            logger.error(EntityUtils.toString(response.getEntity()));
        }
        assertEquals("Couldn't mutate a datastream!", NO_CONTENT.getStatusCode(), status);

    }

    /**
     * Verifies that one version exists with each supplied value. This method makes assertions that each of the
     * provided values is the content of a version node and nothing else. Order isn't important, and no assumption is
     * made about whether extra versions exist.
     */
    protected void verifyVersions(final GraphStore graph, final Node subject, final String... values)
            throws IOException {
        final ArrayList<String> remainingValues = newArrayList(values);
        final Iterator<Quad> versionIt = graph.find(ANY, subject, HAS_VERSION.asNode(), ANY);

        while (versionIt.hasNext() && !remainingValues.isEmpty()) {
            final String value =
                    EntityUtils.toString(execute(new HttpGet(versionIt.next().getObject().getURI()))
                            .getEntity());
            remainingValues.remove(value);
        }

        if (!remainingValues.isEmpty()) {
            fail(remainingValues.get(0) + " was not preserved in the version history!");
        }
    }

}
