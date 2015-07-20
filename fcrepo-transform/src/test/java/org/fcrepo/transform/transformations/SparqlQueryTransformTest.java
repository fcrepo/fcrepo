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
package org.fcrepo.transform.transformations;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * <p>SparqlQueryTransformTest class.</p>
 *
 * @author cbeer
 */
public class SparqlQueryTransformTest {

    @Mock
    Node mockNode;

    @Mock
    Session mockSession;

    SparqlQueryTransform testObj;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        when(mockNode.getSession()).thenReturn(mockSession);
    }

    @Test
    public void testApply() {
        final RdfStream model = new RdfStream();
        model.concat(new Triple(createResource("http://example.org/book/book1").asNode(),
                createProperty("http://purl.org/dc/elements/1.1/title").asNode(),
                createLiteral("some-title")));
        final InputStream query = new ByteArrayInputStream(("SELECT ?title WHERE\n" +
                "{\n" +
                "  <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title .\n" +
                "} ").getBytes());
        testObj = new SparqlQueryTransform(query);

        try (final QueryExecution apply = testObj.apply(model)) {
            assert (apply != null);
            final ResultSet resultSet = apply.execSelect();
            assertTrue(resultSet.hasNext());
            assertEquals("some-title", resultSet.nextSolution().get("title").asLiteral().getValue());
        }
    }

    @Test (expected = IllegalStateException.class)
    public void testApplyException() {
        final RdfStream model = mock(RdfStream.class);
        testObj = new SparqlQueryTransform(null);
        doThrow(IOException.class).when(model).asModel();
        testObj.apply(model);
    }
}
