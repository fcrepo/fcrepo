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
package org.fcrepo.kernel.impl.operations;

import static junit.framework.TestCase.assertTrue;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.SERVER_MANAGED_PROPERTIES_MODE;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Date;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperation;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author bseeger
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateRdfSourceOperationBuilderTest {

    private CreateRdfSourceOperationBuilder builder;

    private Model model;

    private RdfStream stream;

    private static final FedoraId PARENT_ID = FedoraId.create("info:fedora/parent");

    private static final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/test-subject");

    private final Node id = ResourceFactory.createResource(RESOURCE_ID.getResourceId()).asNode();

    private static final String PROPERTY_ID = "http://example.org/isLinkedTo/";

    private final Node prop = ResourceFactory.createProperty(PROPERTY_ID).asNode();

    private static final String OBJECT_VALUE = "Somebody";

    private final Node object = ResourceFactory.createPlainLiteral(OBJECT_VALUE).asNode();

    private final Instant CREATED_INSTANT = Instant.parse("2019-11-12T10:00:30.0Z");

    private final Instant MODIFIED_INSTANT = Instant.parse("2019-11-12T14:11:05.0Z");

    private final String USER_PRINCIPAL = "fedoraUser";

    @Before
    public void setUp() {
        builder = new CreateRdfSourceOperationBuilderImpl(RESOURCE_ID, RDF_SOURCE.toString());
        model = ModelFactory.createDefaultModel();
        model.add(
                ResourceFactory.createResource(RESOURCE_ID.getResourceId()),
                ResourceFactory.createProperty(PROPERTY_ID),
                ResourceFactory.createPlainLiteral(OBJECT_VALUE)
        );
        final OutputStream outputStream = new ByteArrayOutputStream();
        model.write(outputStream, "TURTLE");
        stream = fromModel(id, model);
    }

    @Test
    public void testStream() {
        final RdfSourceOperation op = builder.triples(stream).build();
        assertEquals(CreateRdfSourceOperationImpl.class, op.getClass());
        assertTrue(op.getTriples().anyMatch(t -> t.matches(id, prop, object)));
        assertEquals(stream, op.getTriples());
    }

    @Test
    public void testRelaxedPropertiesAllFields() {
        final var resc = model.getResource(RESOURCE_ID.getResourceId());
        resc.addLiteral(LAST_MODIFIED_DATE, Date.from(MODIFIED_INSTANT));
        resc.addLiteral(LAST_MODIFIED_BY, USER_PRINCIPAL);
        resc.addLiteral(CREATED_DATE, Date.from(CREATED_INSTANT));
        resc.addLiteral(CREATED_BY, USER_PRINCIPAL);

        final RdfSourceOperation op = buildOperationWithRelaxProperties(model);

        assertEquals(USER_PRINCIPAL, op.getCreatedBy());
        assertEquals(USER_PRINCIPAL, op.getLastModifiedBy());
        assertEquals(CREATED_INSTANT, op.getCreatedDate());
        assertEquals(MODIFIED_INSTANT, op.getLastModifiedDate());
    }

    @Test
    public void testRelaxedPropertiesNonDate() {
        final var resc = model.getResource(RESOURCE_ID.getResourceId());
        resc.addLiteral(LAST_MODIFIED_DATE, "Notadate");
        resc.addLiteral(LAST_MODIFIED_BY, USER_PRINCIPAL);
        resc.addLiteral(CREATED_DATE, "Notadate");
        resc.addLiteral(CREATED_BY, USER_PRINCIPAL);

        final RdfSourceOperation op = buildOperationWithRelaxProperties(model);

        assertEquals(USER_PRINCIPAL, op.getCreatedBy());
        assertEquals(USER_PRINCIPAL, op.getLastModifiedBy());
        assertNull(op.getCreatedDate());
        assertNull(op.getLastModifiedDate());
    }

    @Test
    public void testRelaxedPropertiesNotRelaxed() {
        final var resc = model.getResource(RESOURCE_ID.getResourceId());
        resc.addLiteral(LAST_MODIFIED_DATE, Date.from(MODIFIED_INSTANT));
        resc.addLiteral(LAST_MODIFIED_BY, USER_PRINCIPAL);
        resc.addLiteral(CREATED_DATE, Date.from(CREATED_INSTANT));
        resc.addLiteral(CREATED_BY, USER_PRINCIPAL);

        // Relaxed system property not set
        final RdfSourceOperation op = builder.relaxedProperties(model).build();

        assertNull(op.getCreatedBy());
        assertNull(op.getLastModifiedBy());
        assertNull(op.getCreatedDate());
        assertNull(op.getLastModifiedDate());
    }

    @Test
    public void testRelaxedPropertiesNoProperties() {
        final RdfSourceOperation op = buildOperationWithRelaxProperties(model);

        assertNull(op.getCreatedBy());
        assertNull(op.getLastModifiedBy());
        assertNull(op.getCreatedDate());
        assertNull(op.getLastModifiedDate());
    }

    @Test
    public void testArchivalGroupFalseByDefault() {
        final CreateRdfSourceOperation op = builder.build();
        assertFalse(op.isArchivalGroup());
    }

    @Test
    public void testArchivalGroup() {
        final CreateRdfSourceOperation op = builder.archivalGroup(true).build();
        assertTrue(op.isArchivalGroup());
    }


    private RdfSourceOperation buildOperationWithRelaxProperties(final Model model) {
        try {
            System.setProperty(SERVER_MANAGED_PROPERTIES_MODE, "relaxed");
            return builder.relaxedProperties(model).build();
        } finally {
            System.clearProperty(SERVER_MANAGED_PROPERTIES_MODE);
        }
    }

    @Test
    public void testUserPrincipal() {
        final RdfSourceOperation op = builder.userPrincipal(USER_PRINCIPAL).build();

        assertEquals(USER_PRINCIPAL, op.getUserPrincipal());
    }

    @Test
    public void testParentId() {
        final CreateRdfSourceOperation op = builder.parentId(PARENT_ID).build();

        assertEquals(PARENT_ID, op.getParentId());
    }
}
