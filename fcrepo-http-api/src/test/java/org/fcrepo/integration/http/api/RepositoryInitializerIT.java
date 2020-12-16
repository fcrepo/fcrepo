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
package org.fcrepo.integration.http.api;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfLexicon;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import javax.ws.rs.core.Link;
import java.net.ConnectException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author dbernstein
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        TestIsolationExecutionListener.class,
        DirtyContextBeforeAndAfterClassTestExecutionListener.class
}, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public class RepositoryInitializerIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(RepositoryInitializerIT.class);

    static {
        System.setProperty("fcrepo.autoversioning.enabled", "false");
    }

    @Before
    public void init() throws Exception {
        // Because of the dirtied context, need to wait for fedora to restart before testing
        int triesRemaining = 50;
        while (true) {
            final HttpGet get = new HttpGet(serverAddress);
            try (final CloseableHttpResponse response = execute(get)) {
                assertEquals(SC_OK, getStatus(response));
                break;
            } catch (final NoHttpResponseException | ConnectException e) {
                if (triesRemaining-- > 0) {
                    LOGGER.debug("Waiting for fedora to become available");
                    Thread.sleep(50);
                } else {
                    throw new Exception("Fedora instance did not become available in allowed time");
                }
            }
        }
        // Now that fedora has started, clear the property so it won't impact other tests
        System.clearProperty("fcrepo.autoversioning.enabled");
    }

    @Test
    public void testRootResourceIsVersioned() throws Exception {
        final var model = getModel("/fcr:versions");
        final var statements = model.listStatements((Resource) null, RdfLexicon.CONTAINS, (RDFNode) null).toList();
        assertEquals("Should be one version contained by time map", 1, statements.size());
        final var mementURI = statements.get(0).getObject().asResource().getURI();

        assertEquals("The contained link should be a memento", 1,
                getLinkHeaders(new HttpGet(mementURI)).stream()
                        .map(x -> Link.valueOf(x))
                        .filter(x -> x.getRel().equals("type"))
                        .filter(x -> x.getUri().toString().equals(MEMENTO_TYPE)).count());
    }
}
