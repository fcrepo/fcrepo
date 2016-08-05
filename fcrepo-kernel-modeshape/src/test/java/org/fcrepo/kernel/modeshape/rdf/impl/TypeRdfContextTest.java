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
package org.fcrepo.kernel.modeshape.rdf.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.functions.Converter;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.mockResource;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/1/14
 */
public class TypeRdfContextTest {


    @Mock
    private FedoraResourceImpl mockResource;

    @Mock
    private Node mockNode;

    @Mock
    private Node readOnlyNode;

    @Mock
    private NodeType mockPrimaryNodeType;

    @Mock
    private NodeType mockMixinNodeType;

    @Mock
    private NodeType mockPrimarySuperNodeType;

    @Mock
    private NodeType mockMixinSuperNodeType;

    private Converter<Resource, String> idTranslator;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepository;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Workspace mockWorkspace;

    private static final String mockNodeName = "mockNode";

    private static final String mockPrimaryNodeTypeName = "somePrimaryType";
    private static final String mockMixinNodeTypeName = "someMixinType";
    private static final String mockPrimarySuperNodeTypeName = "somePrimarySuperType";
    private static final String mockMixinSuperNodeTypeName = "someMixinSuperType";

    @Before
    public void setUp() {
        initMocks(this);
        final List<URI> types = new ArrayList<>();
        types.add(URI.create(REPOSITORY_NAMESPACE + "somePrimaryType"));
        types.add(URI.create(REPOSITORY_NAMESPACE + "someMixinType"));
        types.add(URI.create(REPOSITORY_NAMESPACE + "somePrimarySuperType"));
        types.add(URI.create(REPOSITORY_NAMESPACE + "someMixinSuperType"));

        when(mockResource.getTypes()).thenReturn(types);
        when(mockResource.getNode()).thenReturn(mockNode);
        mockResource(mockResource, "/" + mockNodeName);

        idTranslator = new DefaultIdentifierTranslator();
    }

    @Test
    public void testRdfTypesForNodetypes() throws IOException {

        final Resource mockNodeSubject = mockResource.asUri(idTranslator);

        try (final TypeRdfContext typeRdfContext = new TypeRdfContext(mockResource, idTranslator)) {
            final Model actual = typeRdfContext.collect(toModel());
            final Resource expectedRdfTypePrimary = createResource(REPOSITORY_NAMESPACE + mockPrimaryNodeTypeName);
            final Resource expectedRdfTypeMixin = createResource(REPOSITORY_NAMESPACE + mockMixinNodeTypeName);
            final Resource expectedRdfTypePrimarySuper =
                    createResource(REPOSITORY_NAMESPACE + mockPrimarySuperNodeTypeName);
            final Resource expectedRdfTypeMixinSuper =
                    createResource(REPOSITORY_NAMESPACE + mockMixinSuperNodeTypeName);
            logRdf("Constructed RDF: ", actual);
            assertTrue("Didn't find RDF type triple for primarytype!", actual.contains(
                    mockNodeSubject, type, expectedRdfTypePrimary));
            assertTrue("Didn't find RDF type triple for mixintype!", actual.contains(
                    mockNodeSubject, type, expectedRdfTypeMixin));
            assertTrue("Didn't find RDF type triple for primarysupertype!", actual.contains(
                    mockNodeSubject, type, expectedRdfTypePrimarySuper));
            assertTrue("Didn't find RDF type triple for mixinsupertype!", actual.contains(
                    mockNodeSubject, type, expectedRdfTypeMixinSuper));
        }
    }

    private static void logRdf(final String message, final Model model) throws IOException {
        LOGGER.debug(message);
        try (Writer w = new StringWriter()) {
            model.write(w);
            LOGGER.debug("\n" + w.toString());
        }
    }

    private static final Logger LOGGER = getLogger(TypeRdfContextTest.class);


}
