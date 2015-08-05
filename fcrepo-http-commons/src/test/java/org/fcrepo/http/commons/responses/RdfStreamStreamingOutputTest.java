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
package org.fcrepo.http.commons.responses;

import static com.google.common.util.concurrent.Futures.addCallback;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static javax.ws.rs.core.MediaType.valueOf;
import static org.fcrepo.http.commons.responses.RdfStreamStreamingOutput.getValueForObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openrdf.model.impl.ValueFactoryImpl.getInstance;
import static org.openrdf.model.util.Literals.createLiteral;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.RDFNode;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * <p>RdfStreamStreamingOutputTest class.</p>
 *
 * @author ajs6f
 */
public class RdfStreamStreamingOutputTest {

    private RdfStreamStreamingOutput testRdfStreamStreamingOutput;

    private static final Triple triple = create(createURI("info:testSubject"),
            createURI("info:testPredicate"), createURI("info:testObject"));

    @Mock
    private Node mockNode;

    private final RdfStream testRdfStream = new RdfStream(triple);

    @Mock
    private RdfStream mockRdfStream;

    private final MediaType testMediaType = valueOf("application/rdf+xml");

    private static final ValueFactory vf = getInstance();

    private static final Logger LOGGER =
            getLogger(RdfStreamStreamingOutputTest.class);

    @Before
    public void setUp() {
        initMocks(this);
        testRdfStreamStreamingOutput =
            new RdfStreamStreamingOutput(testRdfStream, testMediaType);
    }

    @Test
    public void testGetValueForObjectWithResource() {
        final Node resource = createURI("info:test");
        final Value result = getValueForObject(resource);
        assertEquals("Created bad Value!", vf.createURI("info:test"), result);
    }

    @Test
    public void testGetValueForObjectWithLiteral() {
        final Node resource = NodeFactory.createLiteral("test");
        final Value result = getValueForObject(resource);
        assertEquals("Created bad Value!", createLiteral(vf, "test"), result);
    }

    @Test
    public void testWrite() throws IOException {
        assertOutputContainsTriple(triple);
    }

    public void assertOutputContainsTriple(final Triple expected) throws IOException {
        final RdfStream input = new RdfStream(expected);
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, testMediaType).write(output);
            try (
                final InputStream resultStream =
                    new ByteArrayInputStream(output.toByteArray())) {
                final Model result =
                    createDefaultModel().read(resultStream, null);
                assertTrue("Didn't find our test triple!", result
                        .contains(result.asStatement(expected)));
            }
        }
    }

    @Test
    public void testWriteWithNamespace() throws IOException {
        final RdfStream input = new RdfStream().namespace("a", "info:a");
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, RDFMediaType.TURTLE_TYPE).write(output);
            final String s = output.toString("UTF-8");
            assertTrue(s.contains("@prefix a: <info:a>"));
        }
    }


    @Test
    public void testWriteWithXmlnsNamespace() throws IOException {
        final RdfStream input = new RdfStream().namespace("xmlns", "info:a");
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, RDFMediaType.TURTLE_TYPE).write(output);
            final String s = output.toString("UTF-8");
            assertFalse(s.contains("@prefix xmlns"));
        }
    }

    @Test
    public void testWriteWithTypedObject() throws IOException {
        assertOutputContainsTriple(create(createURI("info:testSubject"),
                createURI("info:testPredicate"),
                createTypedLiteral(0).asNode()));
    }

    @Test
    public void testWriteWithBlankSubject() throws IOException {

        final RdfStream input = new RdfStream(create(createResource().asNode(),
                createURI("info:testPredicate"),
                createTypedLiteral(0).asNode()));
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, testMediaType).write(output);

            try (final InputStream resultStream = new ByteArrayInputStream(output.toByteArray())) {
                final Model result = createDefaultModel().read(resultStream, null);
                assertTrue(result.contains(null, createProperty("info:testPredicate"), createTypedLiteral(0)));
            }
        }

    }


    @Test
    public void testWriteWithBlankObject() throws IOException {

        final RdfStream input = new RdfStream(create(createResource().asNode(),
                createURI("info:testPredicate"),
                createResource().asNode()));
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new RdfStreamStreamingOutput(input, testMediaType).write(output);

            try (final InputStream resultStream = new ByteArrayInputStream(output.toByteArray())) {
                final Model result = createDefaultModel().read(resultStream, null);
                assertTrue(result.contains(null, createProperty("info:testPredicate"), (RDFNode)null));
            }
        }

    }


    @Test
    public void testWriteWithDatetimeObject() throws IOException {

        assertOutputContainsTriple(create(createURI("info:testSubject"),
                createURI("info:testPredicate"),
                NodeFactory.createLiteral("2014-01-01T01:02:03Z", XSDDatatype.XSDdateTime)));

    }


    @Test
    public void testWriteWithLanguageLiteral() throws IOException {

        assertOutputContainsTriple(create(createURI("info:testSubject"),
                createURI("info:testPredicate"),
                NodeFactory.createLiteral("french string", "fr")));

    }

    @Test(expected = WebApplicationException.class)
    public void testWriteWithException() throws IOException {

        final FutureCallback<Void> callback = new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void v) {
                throw new AssertionError("Should never happen!");
            }

            @Override
            public void onFailure(final Throwable e) {
                LOGGER.debug("Got exception:", e);
                assertTrue("Got wrong kind of exception!", e instanceof RDFHandlerException);
            }

        };
        addCallback(testRdfStreamStreamingOutput, callback);
        try (final OutputStream mockOutputStream =
                mock(OutputStream.class, new Answer<Object>() {

                    @Override
                    public Object answer(final InvocationOnMock invocation) throws IOException {
                        throw new IOException("Expected.");
                    }
                })) {
            testRdfStreamStreamingOutput.write(mockOutputStream);
        }
    }

}
