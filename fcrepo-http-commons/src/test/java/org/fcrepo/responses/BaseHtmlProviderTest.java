
package org.fcrepo.responses;

import static com.google.common.collect.ImmutableMap.of;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.fcrepo.responses.RdfSerializationUtils.primaryTypePredicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class BaseHtmlProviderTest {

    final BaseHtmlProvider baseHtmlProvider = new BaseHtmlProvider();

    Dataset testData = new DatasetImpl(createDefaultModel());

    {
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(NodeFactory.createURI("test:subject"),
                        createURI("test:predicate"),
                        createLiteral("test:object")));
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"), primaryTypePredicate,
                        createLiteral("nt:file")));

    }

    @Test
    public void testIsWriteable() {
        assertTrue(
                "Gave false response to HtmlProvider.isWriteable() that contained a legitimate combination of parameters!",
                baseHtmlProvider.isWriteable(Dataset.class, Dataset.class, null,
                        TEXT_HTML_TYPE));
        assertFalse(
                "HtmlProvider.isWriteable() should return false if asked to serialize anything other than Dataset!",
                baseHtmlProvider.isWriteable(BaseHtmlProvider.class,
                        BaseHtmlProvider.class, null, TEXT_HTML_TYPE));
        assertFalse(
                "HtmlProvider.isWriteable() should return false to text/plain!",
                baseHtmlProvider.isWriteable(Dataset.class, Dataset.class, null,
                        TEXT_PLAIN_TYPE));
    }

    @Test
    public void testGetSize() {
        assertEquals("Returned wrong size from HtmlProvider!", baseHtmlProvider
                .getSize(null, null, null, null, null), -1);

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testWriteTo() throws WebApplicationException,
            IllegalArgumentException, IOException {
        final Template mockTemplate = mock(Template.class);
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                outStream.write("abcdefighijk".getBytes(), 0, 10);
                return "I am pretending to merge a template for you.";
            }
        }).when(mockTemplate).merge(Mockito.isA(Context.class),
                Mockito.isA(Writer.class));
        baseHtmlProvider.setTemplatesMap(of("nt:file", mockTemplate));
        baseHtmlProvider.writeTo(testData, Dataset.class, mock(Type.class), new Annotation[] {},
                MediaType.valueOf("text/html"),
                (MultivaluedMap) new MultivaluedMapImpl(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue("Got no output from serialization!", results.length > 0);

    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testWriteToWithAnnotation() throws WebApplicationException,
                                             IllegalArgumentException, IOException {
        final Template mockTemplate = mock(Template.class);
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                outStream.write("abcdefighijk".getBytes(), 0, 10);
                return "I am pretending to merge a template for you.";
            }
        }).when(mockTemplate).merge(Mockito.isA(Context.class),
                                           Mockito.isA(Writer.class));
        baseHtmlProvider.setTemplatesMap(of("some:file", mockTemplate));
        HtmlTemplate mockAnnotation = mock(HtmlTemplate.class);
        when(mockAnnotation.value()).thenReturn("some:file");
        baseHtmlProvider.writeTo(testData, Dataset.class, mock(Type.class), new Annotation[] { mockAnnotation },
                                    MediaType.valueOf("text/html"),
                                    (MultivaluedMap) new MultivaluedMapImpl(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue("Got no output from serialization!", results.length > 0);

    }
}
