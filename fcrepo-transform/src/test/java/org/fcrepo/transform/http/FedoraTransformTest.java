/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.transform.http;

import static org.apache.jena.riot.WebContent.contentTypeSPARQLQuery;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.transform.Transformation;
import org.fcrepo.transform.TransformationFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>FedoraTransformTest class.</p>
 *
 * @author cbeer
 */
public class FedoraTransformTest {

    @Mock
    NodeService mockNodeService;

    @Mock
    FedoraResourceImpl mockResource;

    @Mock
    Node mockNode;


    FedoraTransform testObj;

    private Session mockSession;
    private UriInfo uriInfo;

    @Mock
    private TransformationFactory mockTransformationFactory;

    @Mock
    Transformation<Object> mockTransform;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = spy(new FedoraTransform("testObject"));
        setField(testObj, "nodeService", mockNodeService);
        setField(testObj, "transformationFactory", mockTransformationFactory);

        this.uriInfo = getUriInfoImpl();
        setField(testObj, "uriInfo", uriInfo);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);

        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockResource.getPath()).thenReturn("/testObject");
        doReturn(mockResource).when(testObj).getResourceFromPath("testObject");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEvaluateTransform() {
        final RdfStream stream = new RdfStream();
        when(mockResource.getTriples(any(IdentifierConverter.class), any(Class.class))).thenReturn(stream);

        final InputStream query = new ByteArrayInputStream(("SELECT ?title WHERE\n" +
                "{\n" +
                "  <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title .\n" +
                "} ").getBytes());

        when(mockTransformationFactory.getTransform(MediaType.valueOf(contentTypeSPARQLQuery), query)).thenReturn(
                mockTransform);

        testObj.evaluateTransform(MediaType.valueOf(contentTypeSPARQLQuery), query);

        verify(mockTransform).apply(any(RdfStream.class));
    }


}
