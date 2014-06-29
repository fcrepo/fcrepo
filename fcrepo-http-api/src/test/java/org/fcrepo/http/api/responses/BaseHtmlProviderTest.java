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
package org.fcrepo.http.api.responses;

import static com.google.common.collect.ImmutableMap.of;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.primaryTypePredicate;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
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
import javax.ws.rs.core.UriInfo;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * <p>BaseHtmlProviderTest class.</p>
 *
 * @author awoods
 */
public class BaseHtmlProviderTest {

    private final BaseHtmlProvider baseHtmlProvider = new BaseHtmlProvider();

    private final Dataset testData = new DatasetImpl(createDefaultModel());

    @Before
    public void setup() {
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"),
                        createURI("test:predicate"),
                        createLiteral("test:object")));
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"), primaryTypePredicate,
                        createLiteral("nt:file")));

        final UriInfo info = Mockito.mock(UriInfo.class);
        setField(baseHtmlProvider, "uriInfo", info);
    }

    @Test
    public void testIsWriteable() {
        assertTrue(
                "Gave false response to HtmlProvider.isWriteable() that contained legitimate combination of parameters",
                baseHtmlProvider.isWriteable(Dataset.class, Dataset.class,
                        null, TEXT_HTML_TYPE));
        assertFalse(
                "HtmlProvider.isWriteable() should return false if asked to serialize anything other than Dataset!",
                baseHtmlProvider.isWriteable(BaseHtmlProvider.class,
                        BaseHtmlProvider.class, null, TEXT_HTML_TYPE));
        assertFalse(
                "HtmlProvider.isWriteable() should return false to text/plain!",
                baseHtmlProvider.isWriteable(Dataset.class, Dataset.class,
                        null, TEXT_PLAIN_TYPE));
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

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                outStream.write("abcdefighijk".getBytes(), 0, 10);
                return "I am pretending to merge a template for you.";
            }
        }).when(mockTemplate).merge(isA(Context.class), isA(Writer.class));
        setField(baseHtmlProvider, "templatesMap", singletonMap("nt:file",
                mockTemplate));
        baseHtmlProvider.writeTo(testData, Dataset.class, mock(Type.class),
                new Annotation[] {}, MediaType.valueOf("text/html"),
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

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) {
                outStream.write("abcdefighijk".getBytes(), 0, 10);
                return "I am pretending to merge a template for you.";
            }
        }).when(mockTemplate).merge(isA(Context.class), isA(Writer.class));

        setField(baseHtmlProvider, "templatesMap",
                of("some:file", mockTemplate));
        final HtmlTemplate mockAnnotation = mock(HtmlTemplate.class);
        when(mockAnnotation.value()).thenReturn("some:file");
        baseHtmlProvider.writeTo(testData, Dataset.class, mock(Type.class),
                new Annotation[] {mockAnnotation}, MediaType
                        .valueOf("text/html"),
                (MultivaluedMap) new MultivaluedMapImpl(), outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue("Got no output from serialization!", results.length > 0);

    }
}
