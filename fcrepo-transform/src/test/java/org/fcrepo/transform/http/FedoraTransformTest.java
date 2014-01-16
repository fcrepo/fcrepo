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

package org.fcrepo.transform.http;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.WebContent;
import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.http.commons.test.util.TestHelpers;
import org.fcrepo.transform.Transformation;
import org.fcrepo.transform.TransformationFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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
    Transformation mockTransform;

    @Before
    public void setUp() throws NoSuchFieldException, RepositoryException {
        initMocks(this);
        testObj = new FedoraTransform();
        TestHelpers.setField(testObj, "nodeService", mockNodeService);
        TestHelpers.setField(testObj, "transformationFactory", mockTransformationFactory);

        this.uriInfo = getUriInfoImpl();
        TestHelpers.setField(testObj, "uriInfo", uriInfo);
        mockSession = mockSession(testObj);
        TestHelpers.setField(testObj, "session", mockSession);

        when(mockResource.getNode()).thenReturn(mockNode);
    }

    @Test
    public void testEvaluateTransform() throws Exception {
        when(mockNodeService.getObject(mockSession, "/testObject")).thenReturn(mockResource);
        final Model model = ModelFactory.createDefaultModel();
        model.add(model.createResource("http://example.org/book/book1"),
                     model.createProperty("http://purl.org/dc/elements/1.1/title"),
                     model.createLiteral("some-title"));
        final Dataset dataset = DatasetFactory.create(model);
        when(mockResource.getPropertiesDataset(any(GraphSubjects.class))).thenReturn(dataset);

        InputStream query = new ByteArrayInputStream(("SELECT ?title WHERE\n" +
                                                          "{\n" +
                                                          "  <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title .\n" +
                                                          "} ").getBytes());

        when(mockTransformationFactory.getTransform(MediaType.valueOf(WebContent.contentTypeSPARQLQuery), query)).thenReturn(mockTransform);

        testObj.evaluateTransform(createPathList("testObject"), MediaType.valueOf(WebContent.contentTypeSPARQLQuery), query);

        verify(mockTransform).apply(dataset);
    }


}
