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
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.Assert.assertEquals;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateRdfSourceOperationBuilderTest {

    private RdfSourceOperationBuilder builder;

    private Model model;

    private RdfStream stream;

    private static final String RESOURCE_ID = "info:fedora/test-subject";

    private final Node id = ResourceFactory.createResource(RESOURCE_ID).asNode();

    private static final String PROPERTY_ID = "http://example.org/isLinkedTo/";

    private final Node prop = ResourceFactory.createProperty(PROPERTY_ID).asNode();

    private static final String OBJECT_VALUE = "Somebody";

    private final Node object = ResourceFactory.createPlainLiteral(OBJECT_VALUE).asNode();

    @Before
    public void setUp() {
        builder = new CreateRdfSourceOperationBuilderImpl(RESOURCE_ID, RDF_SOURCE.toString());
        model = ModelFactory.createDefaultModel();
        model.add(
                ResourceFactory.createResource(RESOURCE_ID),
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
        assertEquals(CreateRdfSourceOperation.class, op.getClass());
        assertTrue(op.getTriples().anyMatch(t -> t.matches(id, prop, object)));
        assertEquals(stream, op.getTriples());
    }

}
