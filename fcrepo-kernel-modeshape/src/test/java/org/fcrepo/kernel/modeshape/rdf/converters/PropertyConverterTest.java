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
package org.fcrepo.kernel.modeshape.rdf.converters;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.shared.InvalidPropertyURIException;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.modeshape.utils.JcrPropertyMock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import java.util.HashMap;
import java.util.Map;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static java.util.Collections.emptyMap;
import static javax.jcr.PropertyType.REFERENCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 * @author ajs6f
 */
public class PropertyConverterTest {
    public static final Map<String, String> EMPTY_NAMESPACE_MAP = emptyMap();

    @Mock
    private JcrPropertyMock mockNamespacedProperty;

    @Mock
    private Node mockNode;

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNsRegistry;

    private static final String mockUri = "http://example.com/";

    private PropertyConverter testObj;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new PropertyConverter();
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNsRegistry);
        mockNamespaceRegistry(mockNsRegistry);
    }

    @Test
    public final void shouldMapInternalReferencePropertiesToPublicUris() throws RepositoryException {
        when(mockNamespacedProperty.getNamespaceURI()).thenReturn("info:xyz#");
        when(mockNamespacedProperty.getLocalName()).thenReturn(getReferencePropertyName("some_reference"));
        when(mockNamespacedProperty.getType()).thenReturn(REFERENCE);
        when(mockNamespacedProperty.getName()).thenReturn("xyz:" + getReferencePropertyName("some_reference"));
        final Property property = testObj.convert(mockNamespacedProperty);

        assert(property != null);
        assertEquals("some_reference", property.getLocalName());
    }

    @Test(expected = RuntimeException.class)
    public void testGetPredicateForProperty() throws RepositoryException {
        when(mockNamespacedProperty.getNamespaceURI()).thenThrow(new RepositoryException());
        testObj.convert(mockNamespacedProperty);
        fail("Unexpected completion after RepositoryException!");
    }

    @Test
    public final void shouldMapRdfPredicatesToJcrProperties() throws RepositoryException {

        final Property p = createProperty(REPOSITORY_NAMESPACE, "created");
        assertEquals("jcr:created", getPropertyNameFromPredicate(mockNode, p, EMPTY_NAMESPACE_MAP));
    }

    @Test(expected = FedoraInvalidNamespaceException.class)
    public final void shouldRejectFcrPredicates() throws RepositoryException {
        final Property p = createProperty(mockUri, "fcr");
        final Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("fcr", mockUri);
        PropertyConverter.getPropertyNameFromPredicate(mockNode, p, nsMap);
    }

    @Test
    public final void shouldReuseRegisteredNamespaces() throws RepositoryException {
        final Property p = createProperty(mockUri, "uuid");
        assertEquals("some-prefix:uuid", getPropertyNameFromPredicate(mockNode, p,
                EMPTY_NAMESPACE_MAP));
    }

    @Test
    public final void shouldRegisterUnknownUris() throws RepositoryException {
        when(mockNsRegistry.registerNamespace("not-registered-uri#"))
                .thenReturn("ns001");
        final Property p = createProperty("not-registered-uri#", "uuid");
        assertEquals("ns001:uuid", getPropertyNameFromPredicate(mockNode, p, EMPTY_NAMESPACE_MAP));
    }

    @Test (expected = InvalidPropertyURIException.class)
    public void shouldThrowOnForward() {
        final javax.jcr.Property p = mock(javax.jcr.Property.class);
        testObj.convert(p);
    }

    @Test (expected = UnsupportedOperationException.class)
    public void shouldThrowOnBackward() {
        final Property property = testObj.convert(mockNamespacedProperty);
        testObj.doBackward(property);
    }

    private static void mockNamespaceRegistry(final NamespaceRegistry mockRegistry) throws RepositoryException {

        when(mockRegistry.isRegisteredUri(mockUri)).thenReturn(true);
        when(mockRegistry.isRegisteredUri("not-registered-uri#")).thenReturn(
                false);
        when(mockRegistry.isRegisteredUri("http://www.jcp.org/jcr/1.0"))
                .thenReturn(true);
        when(mockRegistry.getPrefix("http://www.jcp.org/jcr/1.0"))
                .thenReturn("jcr");
        when(mockRegistry.getPrefix(mockUri)).thenReturn("some-prefix");
        when(mockRegistry.getURI("jcr")).thenReturn(
                "http://www.jcp.org/jcr/1.0");
        when(mockRegistry.getURI("some-prefix")).thenReturn(mockUri);
        when(mockRegistry.getPrefixes()).thenReturn(
                new String[] {"jcr", "some-prefix"});
    }
}
