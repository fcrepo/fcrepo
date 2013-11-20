
package org.fcrepo.http.commons.responses;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.Session;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Test;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

public class RdfStreamProviderTest {

    private RdfStreamProvider testProvider = new RdfStreamProvider();

    @Test
    public void testGetSize() {
        assertEquals(-1, testProvider.getSize(null, null, null, null, null));
    }

    @Test
    public void testIsWriteable() {
        assertTrue("Should be able to serialize this!", testProvider
                .isWriteable(RdfStream.class, null, null, MediaType
                        .valueOf("application/rdf+xml")));
        assertFalse("Should not be able to serialize this!", testProvider
                .isWriteable(RdfStreamProviderTest.class, null, null, MediaType
                        .valueOf("application/rdf+xml")));
        assertFalse("Should not be able to serialize this!", testProvider
                .isWriteable(RdfStream.class, null, null, MediaType
                        .valueOf("text/html")));
    }

    @Test
    public void testWriteTo() throws WebApplicationException,
                             IllegalArgumentException, IOException {
        final Triple t =
            create(createURI("info:test"), createURI("property:test"),
                    createURI("info:test"));
        final RdfStream rdfStream = new RdfStream(t).session(mock(Session.class));
        byte[] result;
        try (ByteArrayOutputStream entityStream = new ByteArrayOutputStream();) {
            testProvider.writeTo(rdfStream, RdfStream.class, null, null,
                    MediaType.valueOf("application/rdf+xml"), null,
                    entityStream);
            result = entityStream.toByteArray();
        }
        final Model postSerialization =
            createDefaultModel().read(new ByteArrayInputStream(result), null);
        assertTrue("Didn't find our triple!", postSerialization
                .contains(postSerialization.asStatement(t)));
    }

}
