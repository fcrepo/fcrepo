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
package org.fcrepo.http.commons.api.rdf;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.FedoraTimeMap;
import org.fcrepo.kernel.api.models.FedoraWebacAcl;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.modeshape.ContainerImpl;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;
import org.fcrepo.kernel.modeshape.FedoraTimeMapImpl;
import org.fcrepo.kernel.modeshape.FedoraWebacAclImpl;
import org.fcrepo.kernel.modeshape.NonRdfSourceDescriptionImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import javax.ws.rs.core.UriBuilder;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TIME_MAP;
import static org.fcrepo.kernel.api.FedoraTypes.MEMENTO;
import static org.fcrepo.kernel.api.FedoraTypes.MEMENTO_ORIGINAL;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_WEBAC_ACL;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_DESCRIPTION;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author cabeer
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpResourceConverterTest {

    @Mock
    private Session session, txSession;

    @Mock
    private Node node, mementoNode, contentNode, timeMapNode, descriptionNode, webacAclNode;

    @Mock
    private Property mockOriginal;

    private FedoraSession testSession, testTxSession;

    private HttpSession testHttpSession, testHttpBatchSession;

    private HttpResourceConverter converter;
    private final String uriTemplate = "http://localhost:8080/some/{path: .*}";
    private final String path = "arbitrary/path";

    private final String resourceUri = "http://localhost:8080/some/" + path;

    private final Resource resource = createResource(resourceUri);
    private final Resource metadataResource = createResource(resource.toString() + "/fcr:metadata");

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private VersionManager mockVersionManager;

    @Mock
    private VersionHistory mockVersionHistory;

    @Mock
    private Version mockVersion;

    @Before
    public void setUp() throws RepositoryException {
        final UriBuilder uriBuilder = UriBuilder.fromUri(uriTemplate);
        testSession = new FedoraSessionImpl(session);
        testTxSession = new FedoraSessionImpl(txSession);
        testHttpSession = new HttpSession(testSession);
        testHttpBatchSession = new HttpSession(testTxSession);
        testHttpBatchSession.makeBatchSession();
        converter = new HttpResourceConverter(testHttpSession, uriBuilder);

        when(session.getNode("/" + path)).thenReturn(node);
        when(session.getNode("/")).thenReturn(node);
        when(node.getPath()).thenReturn("/" + path);
        when(node.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(false);
        when(descriptionNode.getPath()).thenReturn("/" + path + "/" + FEDORA_DESCRIPTION);
        when(descriptionNode.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(true);
        when(session.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);

        when(mockOriginal.getNode()).thenReturn(node);
        when(timeMapNode.getProperty(MEMENTO_ORIGINAL)).thenReturn(mockOriginal);
        when(timeMapNode.isNodeType(FEDORA_TIME_MAP)).thenReturn(true);
        when(webacAclNode.isNodeType(FEDORA_WEBAC_ACL)).thenReturn(true);
    }

    @Test
    public void testDoForward() {
        final FedoraResource converted = converter.convert(resource);
        assertEquals(node, getJcrNode(converted));
    }

    @Test
    public void testDoForwardWithDatastreamContent() throws Exception {
        when(node.isNodeType(FEDORA_BINARY)).thenReturn(true);
        final FedoraResource converted = converter.convert(resource);
        assertTrue(converted instanceof FedoraBinary);
        assertEquals(node, getJcrNode(converted));
    }

    @Test
    public void testDoForwardWithDatastreamMetadata() throws Exception {
        when(session.getNode("/" + path + "/" + FEDORA_DESCRIPTION)).thenReturn(node);
        when(node.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(true);
        final FedoraResource converted = converter.convert(metadataResource);
        assertTrue(converted instanceof NonRdfSourceDescription);
        assertEquals(node, getJcrNode(converted));
    }

    @Test
    public void testDoForwardWithAHash() throws Exception {
        when(session.getNode("/" + path + "/#/with-a-hash")).thenReturn(node);
        final FedoraResource converted =
                converter.convert(createResource("http://localhost:8080/some/" + path + "#with-a-hash"));
        assertEquals(node, getJcrNode(converted));
    }

    @Test
    public void testDoForwardWithTransaction() throws Exception {
        setField(testTxSession, "id", "xyz");
        final HttpResourceConverter converter = new HttpResourceConverter(testHttpBatchSession,
                UriBuilder.fromUri(uriTemplate));
        when(txSession.getNode("/" + path)).thenReturn(node);
        when(txSession.getWorkspace()).thenReturn(mockWorkspace);
        final Resource resource = createResource("http://localhost:8080/some/tx:xyz/" + path);
        final FedoraResource converted = converter.convert(resource);
        assertEquals(node, getJcrNode(converted));
    }

    @Test
    public void testDoForwardWithUuid() throws Exception {
        final Resource resource = createResource("http://localhost:8080/some/[xyz]");
        when(session.getNode("/[xyz]")).thenReturn(node);
        final FedoraResource converted = converter.convert(resource);
        assertEquals(node, getJcrNode(converted));
    }

    @Test
    public void testDoForwardRoot() {
        final Resource resource = createResource("http://localhost:8080/some/");
        final FedoraResource converted = converter.convert(resource);
        assertEquals(node, getJcrNode(converted));
        assertTrue(converter.inDomain(resource));
    }

    @Test
    public void testDoForwardRootWithoutSlash() {
        final Resource resource = createResource("http://localhost:8080/some");
        final FedoraResource converted = converter.convert(resource);
        assertEquals(node, getJcrNode(converted));
        assertTrue(converter.inDomain(resource));
    }

    @Test
    public void testDoBackward() {
        final Resource converted = converter.reverse().convert(new FedoraResourceImpl(node));
        assertEquals(resource, converted);
    }

    @Test
    public void testDoBackwardWithDatastreamContent() throws Exception {
        when(node.isNodeType(FEDORA_BINARY)).thenReturn(true);
        final Resource converted = converter.reverse().convert(new FedoraBinaryImpl(node));
        assertEquals(resource, converted);
    }

    @Test
    public void testDoBackwardWithDatastreamMetadata() {
        final Resource converted = converter.reverse().convert(new NonRdfSourceDescriptionImpl(descriptionNode));
        assertEquals(metadataResource, converted);
    }

    @Test
    public void testDoBackwardWithHash() throws Exception {
        when(node.getPath()).thenReturn(path + "/#/with-a-hash");
        final Resource converted = converter.reverse().convert(new FedoraResourceImpl(node));
        assertEquals(createResource("http://localhost:8080/some/" + path + "#with-a-hash"), converted);
    }

    @Test
    public void testDoBackwardWithTransaction() throws Exception {
        setField(testTxSession, "id", "xyz");
        final HttpResourceConverter converter = new HttpResourceConverter(testHttpBatchSession,
                UriBuilder.fromUri(uriTemplate));
        when(txSession.getNode("/" + path)).thenReturn(node);
        when(txSession.getWorkspace()).thenReturn(mockWorkspace);
        when(node.getSession()).thenReturn(txSession);
        final Resource resource = createResource("http://localhost:8080/some/tx:xyz/" + path);
        final Resource converted = converter.reverse().convert(new FedoraResourceImpl(node));
        assertEquals(resource, converted);
    }

    @Test
    public void testToStringWithRoot() {
        assertEquals("/", converter.asString(createResource("http://localhost:8080/some/")));
    }

    @Test (expected = InvalidResourceIdentifierException.class)
    public void testToStringWithEmptPathSegment() {
        converter.asString(createResource("http://localhost:8080/some/test/a//b/c/d"));
    }

    @Test
    public void testDoForwardWithTimemap() throws Exception {
        final Resource resource = createResource("http://localhost:8080/some/container/fcr:versions");
        when(session.getNode("/container")).thenReturn(node);

        when(session.getNode("/container/fedora:timemap")).thenReturn(timeMapNode);

        final FedoraResource converted = converter.convert(resource);
        assertTrue("Converted resource must be a timemap", converted instanceof FedoraTimeMap);

        final Node resultNode = getJcrNode(converted);
        assertEquals(timeMapNode, resultNode);
    }

    @Test
    public void testDoForwardWithBinaryTimemap() throws Exception {
        final Resource resource = createResource("http://localhost:8080/some/binary/fcr:versions");
        when(session.getNode("/binary")).thenReturn(node);
        when(node.isNodeType(FEDORA_BINARY)).thenReturn(true);

        when(session.getNode("/binary/fedora:timemap")).thenReturn(timeMapNode);

        final FedoraResource converted = converter.convert(resource);
        assertTrue("Converted resource must be a timemap", converted instanceof FedoraTimeMap);

        final Node resultNode = getJcrNode(converted);
        assertEquals(timeMapNode, resultNode);
    }

    @Test
    public void testDoForwardWithBinaryDescriptionTimemap() throws Exception {
        final Resource resource = createResource("http://localhost:8080/some/binary/fcr:metadata/fcr:versions");
        when(node.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(true);
        when(session.getNode("/binary/fedora:description")).thenReturn(node);

        when(session.getNode("/binary/fedora:description/fedora:timemap")).thenReturn(timeMapNode);

        final FedoraResource converted = converter.convert(resource);
        assertTrue("Converted resource must be a timemap", converted instanceof FedoraTimeMap);

        final Node resultNode = getJcrNode(converted);
        assertEquals(timeMapNode, resultNode);
    }

    @Test
    public void testDoBackWithTimemap() throws Exception {
        final FedoraTimeMap timemap = new FedoraTimeMapImpl(timeMapNode);
        when(timeMapNode.getPath()).thenReturn(path + "/fedora:timemap");

        final Resource converted = converter.reverse().convert(timemap);
        final Resource expectedResource = createResource("http://localhost:8080/some/" + path + "/fcr:versions");
        assertEquals(expectedResource, converted);
    }

    @Test
    public void testDoBackWithBinaryTimemap() throws Exception {
        final FedoraTimeMap timemap = new FedoraTimeMapImpl(timeMapNode);
        when(timeMapNode.getPath()).thenReturn(path + "/fedora:timemap");

        when(node.isNodeType(FEDORA_BINARY)).thenReturn(true);

        final Resource converted = converter.reverse().convert(timemap);
        final Resource expectedResource = createResource(
                "http://localhost:8080/some/" + path + "/fcr:versions");
        assertEquals(expectedResource, converted);
    }

    @Test
    public void testDoBackWithBinaryDescriptionTimemap() throws Exception {
        final FedoraTimeMap timemap = new FedoraTimeMapImpl(timeMapNode);
        when(timeMapNode.getPath()).thenReturn(path + "/fedora:description/fedora:timemap");

        when(node.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(true);

        final Resource converted = converter.reverse().convert(timemap);
        final Resource expectedResource = createResource(
                "http://localhost:8080/some/" + path + "/fcr:metadata/fcr:versions");
        assertEquals(expectedResource, converted);
    }

    @Test
    public void testDoForwardWithMemento() throws Exception {
        final Resource resource = createResource(
                "http://localhost:8080/some/container/fcr:versions/20180315180915");
        when(mementoNode.isNodeType(MEMENTO)).thenReturn(true);
        when(session.getNode("/container/fedora:timemap/20180315180915")).thenReturn(mementoNode);

        when(session.getNode("/container")).thenReturn(node);

        final FedoraResource converted = converter.convert(resource);
        assertTrue("Converted resource must be a container", converted instanceof Container);

        final Node resultNode = getJcrNode(converted);
        assertEquals(mementoNode, resultNode);
    }

    @Test
    public void testDoForwardWithBinaryMemento() throws Exception {
        final Resource resource = createResource(
                "http://localhost:8080/some/binary/fcr:versions/20180315180915");
        when(mementoNode.isNodeType(MEMENTO)).thenReturn(true);
        when(mementoNode.isNodeType(FEDORA_BINARY)).thenReturn(true);
        when(session.getNode("/binary/fedora:timemap/20180315180915")).thenReturn(mementoNode);

        when(session.getNode("/binary")).thenReturn(node);
        when(node.isNodeType(FEDORA_BINARY)).thenReturn(true);

        final FedoraResource converted = converter.convert(resource);
        assertTrue("Converted resource must be a binary", converted instanceof FedoraBinary);

        final Node resultNode = getJcrNode(converted);
        assertEquals(mementoNode, resultNode);
    }

    @Test
    public void testDoForwardWithBinaryDescriptionMemento() throws Exception {
        final Resource resource = createResource(
                "http://localhost:8080/some/binary/fcr:metadata/fcr:versions/20180315180915");
        when(mementoNode.isNodeType(MEMENTO)).thenReturn(true);
        // Timemap for binary description uses fedora:timemap name
        when(session.getNode("/binary/fedora:description/fedora:timemap/20180315180915")).thenReturn(mementoNode);

        when(session.getNode("/binary/fedora:description")).thenReturn(node);

        final FedoraResource converted = converter.convert(resource);
        assertTrue("Converted resource must be a container", converted instanceof Container);

        final Node resultNode = getJcrNode(converted);
        assertEquals(mementoNode, resultNode);
    }

    @Test
    public void testDoBackWithMemento() throws Exception {
        when(node.getPath()).thenReturn("/" + path + "/fedora:timemap/20180315180915");
        when(node.isNodeType(MEMENTO)).thenReturn(true);

        final Container memento = new ContainerImpl(node);

        final Resource converted = converter.reverse().convert(memento);
        final Resource expectedResource = createResource(resourceUri + "/fcr:versions/20180315180915");
        assertEquals(expectedResource, converted);
    }

    @Test
    public void testDoBackWithBinaryMemento() throws Exception {
        when(node.getPath()).thenReturn("/" + path + "/fedora:timemap/20180315180915");
        when(node.isNodeType(FEDORA_BINARY)).thenReturn(true);

        when(node.isNodeType(MEMENTO)).thenReturn(true);

        final FedoraBinary memento = new FedoraBinaryImpl(node);

        final Resource converted = converter.reverse().convert(memento);
        final Resource expectedResource = createResource(resourceUri + "/fcr:versions/20180315180915");
        assertEquals(expectedResource, converted);
    }

    @Test
    public void testDoBackWithBinaryDescriptionMemento() throws Exception {
        when(descriptionNode.getPath()).thenReturn("/" + path + "/fedora:description/fedora:timemap/20180315180915");
        when(descriptionNode.isNodeType(MEMENTO)).thenReturn(true);

        final NonRdfSourceDescription memento = new NonRdfSourceDescriptionImpl(descriptionNode);

        final Resource converted = converter.reverse().convert(memento);
        final Resource expectedResource = createResource(resourceUri + "/fcr:metadata/fcr:versions/20180315180915");
        assertEquals(expectedResource, converted);
    }

    @Test
    public void testDoForwardWithWebacAcl() throws Exception {
        final Resource resource = createResource("http://localhost:8080/some/container/fcr:acl");
        when(session.getNode("/container")).thenReturn(node);

        when(session.getNode("/container/fedora:acl")).thenReturn(webacAclNode);

        final FedoraResource converted = converter.convert(resource);
        assertTrue("Converted resource must be a FedoraWebacAcl", converted instanceof FedoraWebacAcl);

        final Node resultNode = getJcrNode(converted);
        assertEquals(webacAclNode, resultNode);
    }

    @Test
    public void testDoBackWithWebacAcl() throws Exception {
        final FedoraWebacAcl webacAcl = new FedoraWebacAclImpl(webacAclNode);
        when(webacAclNode.getPath()).thenReturn(path + "/fedora:acl");

        final Resource converted = converter.reverse().convert(webacAcl);
        final Resource expectedResource = createResource("http://localhost:8080/some/" + path + "/fcr:acl");
        assertEquals(expectedResource, converted);
    }
}
