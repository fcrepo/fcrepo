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

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.fcrepo.integration.http.api.DirtyContextBeforeAndAfterClassTestExecutionListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class,
    DirtyContextBeforeAndAfterClassTestExecutionListener.class },
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public class RdfNamespaceMappingIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(RdfNamespaceMappingIT.class);

    private static Map<String, String> namespaces = new HashMap<>();;

    static {
        try {
            final File registryFile = File.createTempFile("namespaces", ".yml");
            registryFile.deleteOnExit();

            namespaces.put("ldp", "http://www.w3.org/ns/ldp#");
            namespaces.put("memento", "http://mementoweb.org/ns#");
            namespaces.put("unused", "http://example.com/ns#");
            namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            namespaces.put("fedora", "http://fedora.info/definitions/v4/repository#");

            final String yaml = namespaces.entrySet().stream()
                    .map(n -> n.getKey() + ": " + n.getValue())
                    .collect(Collectors.joining("\n"));
            FileUtils.write(registryFile, yaml, "UTF-8");

            System.setProperty("fcrepo.namespace.registry", registryFile.getAbsolutePath());
            LOGGER.debug("fcrepo.namespace.registry = {}", registryFile.getAbsolutePath());
        } catch (final Exception e) {
            LOGGER.error("Failed to setup namespace configuration file", e);
        }
    }

    @Test
    public void testUnregisteredNamespace() throws Exception {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;

        // create object with rdf that contains a namespace declaration
        final String content = "@prefix asdf: <http://asdf.org/> . <> asdf:foo 'bar' .";
        executeAndClose(putObjMethod(pid, "text/turtle", content));

        // that namespace should come back without a prefix
        final HttpGet httpGet = getObjMethod(pid);
        httpGet.addHeader(ACCEPT, "text/turtle");
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse getResponse = execute(httpGet)) {
            model.read(getResponse.getEntity().getContent(), subjectURI, "TURTLE");

            assertFalse("Should not contain unregistered namespace mapping",
                    model.getNsPrefixMap().containsKey("asdf"));

            final Resource resc = model.getResource(subjectURI);
            assertTrue("Must return property from unregistered namespace",
                    resc.hasLiteral(createProperty("http://asdf.org/foo"), "bar"));
        }
    }

    @Test
    public void testRegisteredNamespace() throws Exception {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObject(pid);

        final HttpGet httpGet = getObjMethod(pid);
        httpGet.addHeader(ACCEPT, "text/turtle");
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse response = execute(httpGet)) {
            model.read(response.getEntity().getContent(), subjectURI, "TURTLE");

            assertTrue("Should contain fedora namespace prefix",
                    model.getNsPrefixMap().containsKey("fedora"));
            assertTrue("Should contain rdf namespace prefix",
                    model.getNsPrefixMap().containsKey("rdf"));

            final Resource resc = model.getResource(subjectURI);
            assertTrue("Must contain property using register namespaces",
                    resc.hasProperty(createProperty(RDF_NAMESPACE + "type"), FEDORA_CONTAINER));
        }
    }

    @Test
    public void testAllNamespacesReturned() throws Exception {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObject(pid);

        final HttpGet httpGet = getObjMethod(pid);
        httpGet.addHeader(ACCEPT, "text/turtle");
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse response = execute(httpGet)) {
            model.read(response.getEntity().getContent(), subjectURI, "TURTLE");

            for (final Entry<String, String> namespace : namespaces.entrySet()) {
                assertTrue("Must contain " + namespace.getKey() + " namespace prefix",
                        model.getNsPrefixMap().containsKey(namespace.getKey()));
                assertEquals(namespace.getValue(), model.getNsPrefixMap().get(namespace.getKey()));
            }
        }
    }
}
