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

import static com.google.common.collect.ImmutableBiMap.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

/**
 * <p>HttpTripleUtilTest class.</p>
 *
 * @author awoods
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpTripleUtilTest {

    private HttpTripleUtil testObj;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private UriAwareResourceModelFactory mockBean1;

    @Mock
    private UriAwareResourceModelFactory mockBean2;

    @Mock
    private ApplicationContext mockContext;

    @Mock
    private FedoraResource mockResource;

    @Before
    public void setUp() {
        testObj = new HttpTripleUtil();
        testObj.setApplicationContext(mockContext);
    }

    @Test
    public void shouldAddTriplesFromRegisteredBeans() {
        final Map<String, UriAwareResourceModelFactory> mockBeans = of("doesnt", mockBean1, "matter", mockBean2);
        when(mockContext.getBeansOfType(UriAwareResourceModelFactory.class)).thenReturn(mockBeans);
        when(mockBean1.createModelForResource(eq(mockResource), eq(mockUriInfo))).thenReturn(
                createDefaultModel());
        when(mockBean2.createModelForResource(eq(mockResource), eq(mockUriInfo))).thenReturn(
                createDefaultModel());

        try (final RdfStream rdfStream = new DefaultRdfStream(createURI("info:subject"))) {

            assertTrue(testObj.addHttpComponentModelsForResourceToStream(rdfStream, mockResource, mockUriInfo)
                    .count() >= 0);

            verify(mockBean1).createModelForResource(eq(mockResource), eq(mockUriInfo));
            verify(mockBean2).createModelForResource(eq(mockResource), eq(mockUriInfo));
        }
    }
}
