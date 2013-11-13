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

import static com.google.common.collect.Iterators.size;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_OF_RESULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraIdentifiersIT extends AbstractResourceIT {

    @Test
    public void testGetNextPidResponds() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "fcr:identifier");
        logger.debug("Executed testGetNextPidResponds()");
        assertEquals(SC_OK, getStatus(method));
    }

    @Test
    public void testGetNextHasAPid() throws IOException {
        final HttpPost method = new HttpPost(serverAddress + "fcr:identifier");
        logger.debug("Executed testGetNextHasAPid()");
        final GraphStore graphStore = getGraphStore(method);
        assertTrue("Didn't find a single dang PID!", graphStore.contains(ANY,
                ResourceFactory.createResource(serverAddress + "fcr:identifier")
                        .asNode(), HAS_MEMBER_OF_RESULT.asNode(), ANY));

    }

    @Test
    public void testGetNextHasTwoPids() throws IOException {
        final HttpPost method =
                new HttpPost(serverAddress + "fcr:identifier?numPids=2");
        method.setHeader("Accept", "application/n3");
        logger.debug("Executed testGetNextHasTwoPids()");
        final GraphStore graphStore = getGraphStore(method);
        assertEquals("Didn't find two dang PIDs!", 2, size(graphStore.find(ANY,
                createResource(serverAddress + "fcr:identifier").asNode(),
                HAS_MEMBER_OF_RESULT.asNode(), ANY)));

    }

    @Test
    public void testGetNextPidRespondsWithPath() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "fcr:identifier");
        logger.debug("Executed testGetNextPidRespondsWithPath()");
        assertEquals(SC_OK, getStatus(method));
    }

    @Test
    public void testGetNextHasAPidWithPath() throws IOException {
        final HttpPost method = new HttpPost(serverAddress + "fcr:identifier");
        logger.debug("Executed testGetNextHasAPidWithPath()");
        final GraphStore graphStore = getGraphStore(method);
        assertTrue("Didn't find a single dang PID!", graphStore.contains(ANY,
                createResource(serverAddress + "fcr:identifier").asNode(),
                HAS_MEMBER_OF_RESULT.asNode(), ANY));

    }

    @Test
    public void testGetNextHasTwoPidsWithPath() throws IOException {
        final HttpPost method =
                new HttpPost(serverAddress + "fcr:identifier?numPids=2");
        logger.debug("Executed testGetNextHasTwoPidsWithPath()");
        final GraphStore graphStore = getGraphStore(method);
        assertEquals("Didn't find two dang PIDs!", 2, size(graphStore.find(ANY,
                createResource(serverAddress + "fcr:identifier").asNode(),
                HAS_MEMBER_OF_RESULT.asNode(), ANY)));

    }
}
