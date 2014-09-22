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
package org.fcrepo.http.api;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.Response.notModified;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockDatastream;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.VersionService;

import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.google.common.collect.ImmutableMap;

/**
 * <p>FedoraBatchTest class.</p>
 *
 * @author cbeer
 */
public class FedoraBatchTest {

    FedoraBatch testObj;

    @Mock
    private DatastreamService mockDatastreams;

    @Mock
    private NodeService mockNodes;

    @Mock
    private VersionService mockVersions;

    @Mock
    private ObjectService mockObjects;

    @Mock
    private Node mockDsNode;

    @Mock
    private NodeType mockDsNodeType;

    @Mock
    private FedoraObject mockObject;

    @Mock
    private NodeIterator mockIterator;

    @Mock
    private Request mockRequest;

    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private Datastream mockDatastream;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraBatch();
        setField(testObj, "objectService", mockObjects);
        setField(testObj, "datastreamService", mockDatastreams);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "versionService", mockVersions);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);
        when(mockDsNodeType.getName()).thenReturn("nt:file");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockDsNodeType);
        when(mockDatastream.getNode()).thenReturn(mockNode);
    }

    @Test
    public void testBatchSparqlUpdate() throws Exception {
        final String pid = "FedoraDatastreamsTest1";

        when(mockNode.getPath()).thenReturn("/FedoraDatastreamsTest1");
        when(mockObjects.findOrCreateObject(mockSession, "/{}FedoraDatastreamsTest1")).thenReturn(mockObject);
        when(mockSession.getNode("/FedoraDatastreamsTest1")).thenReturn(
                                                                           mockNode);

        final MultiPart multipart = new MultiPart();

        final StreamDataBodyPart part = new StreamDataBodyPart(".",
                                                               IOUtils.toInputStream("xyz"),
                                                               null,
                                                               MediaType.valueOf(contentTypeSPARQLUpdate));

        try {
            final FormDataContentDisposition cd =
                new FormDataContentDisposition("form-data; name=\".\"");
            part.contentDisposition(cd);
        } catch (final ParseException ex) {
            ex.printStackTrace();
        }

        multipart.bodyPart(part);

        testObj.batchModify(createPathList(pid), multipart);
        verify(mockObject).updatePropertiesDataset(any(IdentifierTranslator.class), eq("xyz"));
        verify(mockSession).save();
    }

    @Test
    public void testBatchRdfPost() throws Exception {
        final String pid = "FedoraDatastreamsTest1";

        final String path = "/FedoraDatastreamsTest1";
        when(mockNode.getPath()).thenReturn(path);
        when(mockObjects.findOrCreateObject(mockSession, "/{}FedoraDatastreamsTest1")).thenReturn(mockObject);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockObject.getPath()).thenReturn(path);
        when(mockSession.getNode(path)).thenReturn(mockNode);

        final MultiPart multipart = new MultiPart();

        final StreamDataBodyPart part = new StreamDataBodyPart(".",
                                                               IOUtils.toInputStream("<> <info:a> 'xyz'"),
                                                               null,
                                                               MediaType.valueOf(contentTypeTurtle));

        try {
            final FormDataContentDisposition cd =
                new FormDataContentDisposition("form-data; name=\".\"");
            part.contentDisposition(cd);
        } catch (final ParseException ex) {
            ex.printStackTrace();
        }

        multipart.bodyPart(part);

        testObj.batchModify(createPathList(pid), multipart);
        final ArgumentCaptor<Model> captor = ArgumentCaptor.forClass(Model.class);
        verify(mockObject).replaceProperties(any(IdentifierTranslator.class), captor.capture(), any(RdfStream.class));
        final Model capturedModel = captor.getValue();
        assertTrue(capturedModel.contains(capturedModel.createResource("http://localhost/fcrepo/" + pid),
                                             capturedModel.createProperty("info:a"),
                                             capturedModel.createLiteral("xyz")));
        verify(mockSession).save();
    }

    @Test
    public void testModifyBinaryContent() throws Exception {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId1 = "testDs1";
        final String dsId2 = "testDs2";
        final Map<String, String> atts =
            ImmutableMap.of(dsId1, "asdf", dsId2, "sdfg");
        final MultiPart multipart = getStringsAsMultipart(atts);
        when(mockNode.getPath()).thenReturn("/FedoraDatastreamsTest1");
        when(mockSession.getNode("/FedoraDatastreamsTest1")).thenReturn(
                mockNode);

        when(mockDatastreams.findOrCreateDatastream(any(Session.class), eq("/{}" + pid + "/{}" + dsId1)))
                .thenReturn(mockDatastream);
        final Datastream mockDatastream2 = mock(Datastream.class);
        when(mockDatastreams.findOrCreateDatastream(any(Session.class), eq("/{}" + pid + "/{}" + dsId2)))
                .thenReturn(mockDatastream2);

        final FedoraBinary mockBinary1 = mock(FedoraBinary.class);
        final FedoraBinary mockBinary2 = mock(FedoraBinary.class);
        when(mockDatastream.getBinary()).thenReturn(mockBinary1);
        when(mockDatastream2.getBinary()).thenReturn(mockBinary2);

        final Response actual =
            testObj.batchModify(createPathList(pid), multipart);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockBinary1).setContent(any(InputStream.class), anyString(), eq((URI)null), eq("testDs1.txt"),
                any(StoragePolicyDecisionPoint.class));
        verify(mockBinary2).setContent(any(InputStream.class), anyString(), eq((URI)null), eq("testDs2.txt"),
                any(StoragePolicyDecisionPoint.class));

        verify(mockSession).save();
    }

    @Test
    public void testModifyBinaryRdfContent() throws Exception {
        final String pid = "FedoraDatastreamsTest1";

        final MultiPart multipart = new MultiPart();

        final StreamDataBodyPart part = new StreamDataBodyPart("xyz",
                                                               IOUtils.toInputStream("<> <info:a> 'xyz'"),
                                                               "filename.txt",
                                                               MediaType.valueOf(contentTypeTurtle));

        try {
            final FormDataContentDisposition cd =
                new FormDataContentDisposition("form-data; name=xyz;filename=\"filename.txt\"");
            part.contentDisposition(cd);
        } catch (final ParseException ex) {
            ex.printStackTrace();
        }

        multipart.bodyPart(part);

        when(mockDatastreams.findOrCreateDatastream(any(Session.class), eq("/{}" + pid + "/{}xyz")))
                .thenReturn(mockDatastream);

        final FedoraBinary mockBinary = mock(FedoraBinary.class);
        when(mockDatastream.getBinary()).thenReturn(mockBinary);

        when(mockNode.getPath()).thenReturn("/FedoraDatastreamsTest1");
        when(mockSession.getNode("/FedoraDatastreamsTest1")).thenReturn(mockNode);
        final Response actual =
            testObj.batchModify(createPathList(pid), multipart);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockBinary).setContent(any(InputStream.class), eq("text/turtle"), eq((URI) null), eq("filename.txt"),
                any(StoragePolicyDecisionPoint.class));
        verify(mockSession).save();
    }

    @Test
    public void testBatchDelete() throws Exception {
        final String pid = "FedoraDatastreamsTest1";

        when(mockNode.getPath()).thenReturn("/FedoraDatastreamsTest1");
        when(mockSession.getNode("/FedoraDatastreamsTest1")).thenReturn(mockNode);

        when(mockNodes.getObject(isA(Session.class), eq("/{}FedoraDatastreamsTest1/{}xyz"))).thenReturn(mockObject);

        final MultiPart multipart = new MultiPart();

        final StreamDataBodyPart part = new StreamDataBodyPart("delete[]", IOUtils.toInputStream("xyz"));

        try {
            final FormDataContentDisposition cd =
                new FormDataContentDisposition("form-data; name=\"delete[]\"");
            part.contentDisposition(cd);
        } catch (final ParseException ex) {
            ex.printStackTrace();
        }

        multipart.bodyPart(part);

        testObj.batchModify(createPathList(pid), multipart);
        verify(mockObject).delete();
        verify(mockSession).save();
    }

    @Test
    public void testDeleteDatastreams() throws RepositoryException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/" + pid;
        final List<String> dsidList = asList("ds1", "ds2");

        when(mockNodes.getObject(isA(Session.class), eq("/FedoraDatastreamsTest1/ds1"))).thenReturn(mockObject);
        when(mockNodes.getObject(isA(Session.class), eq("/FedoraDatastreamsTest1/ds2"))).thenReturn(mockDatastream);

        final Response actual =
            testObj.batchDelete(createPathList(pid), dsidList);

        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockObject).delete();
        verify(mockDatastream).delete();
        verify(mockSession).save();
    }

    @Test
    public void testGetDatastreamsContents() throws RepositoryException,
                                            IOException,
                                            NoSuchAlgorithmException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final Datastream mockDs = mockDatastream(pid, dsId, dsContent);
        when(mockDs.hasContent()).thenReturn(true);
        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockIterator.nextNode()).thenReturn(mockDsNode);
        when(mockIterator.getSize()).thenReturn(1L);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getNodes(new String[] {dsId})).thenReturn(mockIterator);
        when(mockNodes.getObject(mockSession, "/FedoraDatastreamsTest1"))
                .thenReturn(mockObject);
        when(mockDatastreams.asDatastream(mockDsNode)).thenReturn(mockDs);

        final Response resp =
            testObj.getBinaryContents(createPathList(pid), asList(dsId),
                                         mockRequest);
        final MultiPart multipart = (MultiPart) resp.getEntity();

        verify(mockDs.getBinary()).getContent();
        verify(mockSession, never()).save();
        assertEquals(1, multipart.getBodyParts().size());
        try (final InputStream actualContent =
                (InputStream) multipart.getBodyParts().get(0).getEntity()) {
            assertEquals("/FedoraDatastreamsTest1/testDS", multipart.getBodyParts()
                    .get(0).getContentDisposition().getFileName());
            assertEquals("asdf", IOUtils.toString(actualContent, "UTF-8"));
        }
    }

    @Test
    public void testGetDatastreamsContentsCached() throws RepositoryException,
                                                  NoSuchAlgorithmException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final Datastream mockDs = mockDatastream(pid, dsId, dsContent);
        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockIterator.nextNode()).thenReturn(mockDsNode);
        when(mockIterator.getSize()).thenReturn(1L);
        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getNodes(new String[] {dsId})).thenReturn(mockIterator);
        when(mockNodes.getObject(mockSession, "/FedoraDatastreamsTest1"))
                .thenReturn(mockObject);
        when(mockDatastreams.asDatastream(mockDsNode)).thenReturn(mockDs);
        when(
                mockRequest.evaluatePreconditions(any(Date.class),
                        any(EntityTag.class))).thenReturn(notModified());

        final Response resp =
            testObj.getBinaryContents(createPathList(pid), asList(dsId),
                                         mockRequest);
        verify(mockDs.getBinary(), never()).getContent();
        verify(mockSession, never()).save();
        assertEquals(NOT_MODIFIED.getStatusCode(), resp.getStatus());
    }

    public static MultiPart getStringsAsMultipart(
            final Map<String, String> contents) {
        final MultiPart multipart = new MultiPart();
        for (final Map.Entry<String, String> e : contents.entrySet()) {
            final String id = e.getKey();
            final String content = e.getValue();
            final InputStream src = IOUtils.toInputStream(content);
            final StreamDataBodyPart part = new StreamDataBodyPart(id, src);
            try {
                final FormDataContentDisposition cd =
                    new FormDataContentDisposition("form-data;name=" + id
                            + ";filename=" + id + ".txt");
                part.contentDisposition(cd);
            } catch (final ParseException ex) {
                ex.printStackTrace();
            }
            multipart.bodyPart(part);
        }
        return multipart;
    }

}
