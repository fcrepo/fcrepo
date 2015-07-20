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
package org.fcrepo.http.commons.api.rdf;

import static com.google.common.collect.ImmutableBiMap.of;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * <p>HttpTripleUtilTest class.</p>
 *
 * @author awoods
 */
public class HttpTripleUtilTest {

    private HttpTripleUtil testObj;


    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private IdentifierConverter<Resource,FedoraResource> mockSubjects;

    @Mock
    private UriAwareResourceModelFactory mockBean1;

    @Mock
    private UriAwareResourceModelFactory mockBean2;

    @Mock
    private ApplicationContext mockContext;

    @Mock
    private FedoraResourceImpl mockResource;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new HttpTripleUtil();
        testObj.setApplicationContext(mockContext);
    }

    @Test
    public void shouldAddTriplesFromRegisteredBeans() {
        final Map<String, UriAwareResourceModelFactory> mockBeans =
                of("doesnt", mockBean1, "matter", mockBean2);
        when(mockContext.getBeansOfType(UriAwareResourceModelFactory.class))
                .thenReturn(mockBeans);
        when(
                mockBean1.createModelForResource(eq(mockResource),
                        eq(mockUriInfo), eq(mockSubjects))).thenReturn(
                ModelFactory.createDefaultModel());
        when(
                mockBean2.createModelForResource(eq(mockResource),
                        eq(mockUriInfo), eq(mockSubjects))).thenReturn(
                createDefaultModel());

        final RdfStream rdfStream = new RdfStream();
        testObj.addHttpComponentModelsForResourceToStream(rdfStream, mockResource,
                mockUriInfo, mockSubjects);
        verify(mockBean1).createModelForResource(eq(mockResource),
                eq(mockUriInfo), eq(mockSubjects));
        verify(mockBean2).createModelForResource(eq(mockResource),
                eq(mockUriInfo), eq(mockSubjects));
    }
}
