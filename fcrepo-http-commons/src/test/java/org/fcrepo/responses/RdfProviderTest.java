
package org.fcrepo.responses;

import static com.hp.hpl.jena.graph.Node.createLiteral;
import static com.hp.hpl.jena.graph.Node.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.fcrepo.responses.RdfSerializationUtils.primaryTypePredicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class RdfProviderTest {

    final RdfProvider rdfProvider = new RdfProvider();

    Dataset testData = new DatasetImpl(createDefaultModel());

    {
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"),
                        createURI("test:predicate"),
                        createLiteral("test:object")));
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"), primaryTypePredicate,
                        createLiteral("nt:file")));

    }

    @Test
    public void testIsWriteable() {
        assertTrue(
                "Gave false response to RdfProvider.isWriteable() that contained a legitimate combination of parameters!",
                rdfProvider.isWriteable(Dataset.class, Dataset.class, null,
                        MediaType.valueOf("application/rdf+xml")));
        assertFalse(
                "RdfProvider.isWriteable() should return false if asked to serialize anything other than Dataset!",
                rdfProvider.isWriteable(RdfProvider.class, RdfProvider.class,
                        null, MediaType.valueOf("application/rdf+xml")));
        assertFalse(
                "RdfProvider.isWriteable() should return false to text/html!",
                rdfProvider.isWriteable(Dataset.class, Dataset.class, null,
                        TEXT_HTML_TYPE));
    }

    @Test
    public void testGetSize() {
        assertEquals("Returned wrong size from HtmlProvider!", rdfProvider
                .getSize(null, null, null, null, null), -1);

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testWriteTo() throws WebApplicationException,
            IllegalArgumentException, IOException {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        rdfProvider.writeTo(testData, Dataset.class, mock(Type.class), null,
                MediaType.valueOf("application/rdf+xml"),
                (MultivaluedMap) new MultivaluedMapImpl(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue("Got no output from serialization!", results.length > 0);
        assertTrue("Couldn't find test RDF-object mentioned!", new String(
                results).contains("test:object"));
    }
}
