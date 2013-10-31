package org.fcrepo.http.commons.responses;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static javax.ws.rs.core.MediaType.valueOf;
import static org.fcrepo.http.commons.responses.RdfStreamStreamingOutput.getValueForObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openrdf.model.impl.ValueFactoryImpl.getInstance;
import static org.openrdf.model.util.Literals.createLiteral;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;

import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;


public class RdfStreamStreamingOutputTest {

    private RdfStreamStreamingOutput testRdfStreamStreamingOutput;

    private static final Triple triple = create(createURI("info:testSubject"),
            createURI("info:testPredicate"), createURI("info:testObject"));

    private RdfStream testRdfStream = new RdfStream(triple);

    private MediaType testMediaType = valueOf("application/rdf+xml");

    private static final ValueFactory vf = getInstance();

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
                final Model result = createDefaultModel().read(resultStream, null);
                assertTrue("Didn't find our test triple!", result
                        .contains(result.asStatement(triple)));
            }
        }
    }

}
