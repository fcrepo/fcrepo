/**
 * Copyright 2013 DuraSpace, Inc.
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

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.query.Dataset;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import com.hp.hpl.jena.graph.Triple;

public class StreamingBaseHtmlProviderTest {

    private StreamingBaseHtmlProvider testProvider = new StreamingBaseHtmlProvider();

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private BaseHtmlProvider mockBaseHtmlProvider;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                                                                 mockNamespaceRegistry);
        when(mockNamespaceRegistry.getPrefixes()).thenReturn(new String[]{ "pr"});
        when(mockNamespaceRegistry.getURI("pr")).thenReturn("nsuri");

        setField(testProvider, "delegate", mockBaseHtmlProvider);
    }

    @Test
    public void testGetSize() {
        assertEquals(-1, testProvider.getSize(null, null, null, null, null));
    }

    @Test
    public void testIsWriteable() {
        when(mockBaseHtmlProvider.isWriteable(Dataset.class, null, null, MediaType.valueOf("text/something-like-html"))).thenReturn(true);
        assertTrue(testProvider.isWriteable(RdfStream.class, null, null, MediaType.valueOf("text/something-like-html")));
    }

    @Test
    public void testWriteTo() throws WebApplicationException,
                                         IllegalArgumentException, IOException {
        final Triple t =
            create(createURI("info:test"), createURI("property:test"),
                      createURI("info:test"));
        final RdfStream rdfStream = new RdfStream(t).session(mockSession);
        try (ByteArrayOutputStream entityStream = new ByteArrayOutputStream()) {
            testProvider.writeTo(rdfStream, RdfStream.class, null, null,
                                    MediaType.valueOf("text/html"), null,
                                    entityStream);
        }

        // check that the BaseHtmlProvider gets a dataset with namespace prefixes set
        final ArgumentCaptor<Dataset> argument = ArgumentCaptor.forClass(Dataset.class);
        verify(mockBaseHtmlProvider).writeTo(argument.capture(),
                                                eq(RdfStream.class),
                                                eq((Type)null),
                                                eq((Annotation[])null),
                                                eq(MediaType.valueOf("text/html")),
                                                eq((MultivaluedMap<String, Object>)null),
                                                any(OutputStream.class));
        assertEquals(ImmutableMap.of("pr", "nsuri"), argument.getValue().getDefaultModel().getNsPrefixMap());
    }

}
