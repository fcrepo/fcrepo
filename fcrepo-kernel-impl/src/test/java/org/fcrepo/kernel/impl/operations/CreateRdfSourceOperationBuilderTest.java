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
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.junit.Assert.assertEquals;

import org.apache.commons.io.IOUtils;
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
import java.io.InputStream;
import java.io.OutputStream;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateRdfSourceOperationBuilderTest {

    private RdfSourceOperationBuilder builder;

    private Model model;

    private String turtleGraph;

    private RdfStream stream;

    private final String resourceId = "info:fedora/test-subject";

    private final Node id = ResourceFactory.createResource(resourceId).asNode();

    private final String propertyId = "http://example.org/isLinkedTo/";

    private final Node prop = ResourceFactory.createProperty(propertyId).asNode();

    private final String objectValue = "Somebody";

    private final Node object = ResourceFactory.createPlainLiteral(objectValue).asNode();

    @Before
    public void setUp() {
        builder = new CreateRdfSourceOperationBuilder(resourceId);
        model = ModelFactory.createDefaultModel();
        model.add(
                ResourceFactory.createResource(resourceId),
                ResourceFactory.createProperty(propertyId),
                ResourceFactory.createPlainLiteral(objectValue)
        );
        final OutputStream outputStream = new ByteArrayOutputStream();
        model.write(outputStream, "TURTLE");
        turtleGraph = outputStream.toString();
        stream = fromModel(id, model);
    }

    @Test
    public void testStream() {
        final RdfSourceOperation op = builder.triples(stream).build();
        assertEquals(CreateRdfSourceOperation.class, op.getClass());
        assertTrue(op.getTriples().anyMatch(t -> t.matches(id, prop, object)));
        assertEquals(stream, op.getTriples());
    }

    @Test
    public void testContent() throws Exception {
        final InputStream input = IOUtils.toInputStream(turtleGraph, "UTF-8");
        final RdfSourceOperation op = builder.triples(input, "text/turtle").build();
        assertEquals(CreateRdfSourceOperation.class, op.getClass());
        assertTrue(op.getTriples().anyMatch(t -> t.matches(id, prop, object)));
    }
}
