/**
 * Copyright 2014 DuraSpace, Inc.
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
import static javax.ws.rs.core.MediaType.valueOf;
import static org.fcrepo.http.commons.responses.RdfStreamStreamingOutput.getValueForObject;
import static org.junit.Assert.assertEquals;
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

import org.fcrepo.kernel.utils.iterators.RdfStream;
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

    private RdfStream testRdfStream = new RdfStream(triple);

    @Mock
    private RdfStream mockRdfStream;

    private MediaType testMediaType = valueOf("application/rdf+xml");

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

    @Test(expected = UnsupportedOperationException.class)
    public void testGetValueForObjectWithBlank() {
        final Node resource = NodeFactory.createAnon();
        getValueForObject(resource);
    }

    @Test
    public void testWrite() throws IOException {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            testRdfStreamStreamingOutput.write(output);
            try (
                final InputStream resultStream =
                    new ByteArrayInputStream(output.toByteArray())) {
                final Model result =
                    createDefaultModel().read(resultStream, null);
                assertTrue("Didn't find our test triple!", result
                        .contains(result.asStatement(triple)));
            }
        }
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
                assertTrue("Got wrong kind of exception!",
                        e instanceof RDFHandlerException);
            }

        };
        addCallback(testRdfStreamStreamingOutput, callback);
        final OutputStream mockOutputStream =
            mock(OutputStream.class, new Answer<Object>() {

                @Override
                public Object answer(final InvocationOnMock invocation)
                    throws IOException {
                    throw new IOException("Expected.");
                }
            });

        testRdfStreamStreamingOutput.write(mockOutputStream);
    }

}
