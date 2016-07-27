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

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.NonRdfSourceDescriptionImpl;
import org.fcrepo.kernel.modeshape.testutilities.TestPropertyIterator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * PropertiesRdfContextTest class.
 *
 * @author awoods
 * @author ajs6f
 * @since 2015-03-08
 */
public class PropertiesRdfContextTest {

    @Mock
    private FedoraResourceImpl mockResource;

    @Mock
    private FedoraBinaryImpl mockBinary;

    @Mock
    private Node mockResourceNode;

    @Mock
    private Node mockBinaryNode;

    @Mock
    private NonRdfSourceDescriptionImpl mockNonRdfSourceDescription;

    @Mock
    private Session mockSession;

    @Mock
    private PropertyIterator mockPropertyIterator;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    private static final String RDF_PROPERTY_NAME = "rdf-property-name";

    private static final String RDF_PROPERTY_VALUE = "test:rdf-value";

    private static final String BINARY_PROPERTY_NAME = "binary-property-name";

    private static final String BINARY_PROPERTY_VALUE = "test:binary-value";

    private static final String RDF_PATH = "/resource/path";

    private static final String BINARY_PATH = "/binary/path";

    private static final Statement RDF_SOURCE_STMT = createStatement(
            createResource("info:fedora" + RDF_PATH),
            createProperty(RDF_PROPERTY_NAME),
            createResource(RDF_PROPERTY_VALUE));

    private static final Statement NON_RDF_SOURCE_STMT = createStatement(
            createResource("info:fedora" + BINARY_PATH),
            createProperty(BINARY_PROPERTY_NAME),
            createResource(BINARY_PROPERTY_VALUE));

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        // Mock RDF Source
        when(mockResource.getNode()).thenReturn(mockResourceNode);
        when(mockResourceNode.getSession()).thenReturn(mockSession);
        when(mockResource.getPath()).thenReturn(RDF_PATH);

        when(mockResourceNode.getPath()).thenReturn(RDF_PATH);

        // Mock NonRDF Source
        when(mockBinary.getNode()).thenReturn(mockBinaryNode);
        when(mockBinaryNode.getSession()).thenReturn(mockSession);
        when(mockBinary.getPath()).thenReturn(BINARY_PATH);

        when(mockBinary.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockNonRdfSourceDescription.getNode()).thenReturn(mockResourceNode);

        when(mockBinaryNode.getPath()).thenReturn(BINARY_PATH);

        idTranslator = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testFedoraBinaryProperties() throws RepositoryException {
        final Property mockNonRdfProperty = mock(Property.class);
        final PropertyDefinition mockNonRdfPropertyDefinition = mock(PropertyDefinition.class);
        when(mockNonRdfPropertyDefinition.isProtected()).thenReturn(false);
        when(mockNonRdfProperty.getDefinition()).thenReturn(mockNonRdfPropertyDefinition);

        when(mockNonRdfProperty.getParent()).thenReturn(mockBinaryNode);
        final Value mockNonRdfPropertyValue = mock(Value.class);

        when(mockNonRdfPropertyValue.getType()).thenReturn(PropertyType.URI);
        when(mockNonRdfProperty.getValue()).thenReturn(mockNonRdfPropertyValue);
        when(mockNonRdfProperty.getName()).thenReturn(BINARY_PROPERTY_NAME);
        when(mockNonRdfPropertyValue.getString()).thenReturn(BINARY_PROPERTY_VALUE);

        when(mockBinaryNode.getProperties()).thenReturn(new TestPropertyIterator(mockNonRdfProperty));
        when(mockPropertyIterator.next()).thenReturn(mockNonRdfProperty);
        when(mockPropertyIterator.hasNext()).thenReturn(true, false);

        try (final PropertiesRdfContext propertiesRdfContext = new PropertiesRdfContext(mockBinary, idTranslator)) {
            final Model results = propertiesRdfContext.collect(toModel());

            final Resource correctSubject = idTranslator.reverse().convert(mockBinary);

            results.listStatements().forEachRemaining(stmnt -> assertEquals(
                    "All subjects in triples created should be the resource processed!",
                    correctSubject, stmnt.getSubject()));
            assertTrue("Should contain NonRdfSource statement: " + results + " -- " + NON_RDF_SOURCE_STMT,
                    results.contains(NON_RDF_SOURCE_STMT));
        }
    }

    @Test
    public void testFedoraResourceProperties() throws RepositoryException {
        final Property mockResourceProperty = mock(Property.class);
        final PropertyDefinition mockPropertyDefinition = mock(PropertyDefinition.class);
        when(mockPropertyDefinition.isProtected()).thenReturn(false);
        when(mockResourceProperty.getDefinition()).thenReturn(mockPropertyDefinition);

        when(mockResourceProperty.getParent()).thenReturn(mockResourceNode);
        final Value mockResourcePropertyValue = mock(Value.class);

        when(mockResourcePropertyValue.getType()).thenReturn(PropertyType.URI);
        when(mockResourceProperty.getValue()).thenReturn(mockResourcePropertyValue);
        when(mockResourceProperty.getName()).thenReturn(RDF_PROPERTY_NAME);
        when(mockResourcePropertyValue.getString()).thenReturn(RDF_PROPERTY_VALUE);
        when(mockResourceNode.getProperties()).thenReturn(new TestPropertyIterator(mockResourceProperty));
        when(mockPropertyIterator.next()).thenReturn(mockResourceProperty);
        when(mockPropertyIterator.hasNext()).thenReturn(true, false);

        try (final PropertiesRdfContext propertiesRdfContext = new PropertiesRdfContext(mockResource, idTranslator)) {
            final Model results = propertiesRdfContext.collect(toModel());

            assertTrue("Should contain RdfSource statement: " + results + " -- " + RDF_SOURCE_STMT,
                    results.contains(RDF_SOURCE_STMT));

            assertFalse("Should NOT contain NonRdfSource statement: " + results + " -- " + NON_RDF_SOURCE_STMT,
                    results.contains(NON_RDF_SOURCE_STMT));
        }
    }

}
