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
package org.fcrepo.http.api.url;

import static com.google.common.collect.ImmutableSet.of;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.kernel.api.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION_HISTORY;
import static org.jgroups.util.Util.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.serialization.SerializerUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Session;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>HttpApiResourcesTest class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
public class HttpApiResourcesTest {

    private HttpApiResources testObj;

    private UriInfo uriInfo;

    private HttpResourceConverter mockSubjects;

    @Mock
    private SerializerUtil mockSerializers;

    @Mock
    FedoraObjectSerializer mockSerializer;

    @Mock
    private Session mockSession;

    @Mock
    private FedoraResource mockResource;

    @Mock
    private FedoraBinary mockBinary;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new HttpApiResources();
        uriInfo = getUriInfoImpl();
        mockSubjects = new HttpResourceConverter(mockSession, UriBuilder.fromUri("http://localhost/{path: .*}"));
        setField(testObj, "serializers", mockSerializers);
        final Set<String> serializerKeySet = new HashSet<>();
        final String format = "DummyFORMAT";
        serializerKeySet.add(format);
        when(mockSerializers.keySet()).thenReturn(serializerKeySet);
        when(mockSerializers.getSerializer(any(String.class))).thenReturn(mockSerializer);
        when(mockSerializer.canSerialize(mockResource)).thenReturn(true);
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks() {
        when(mockResource.hasType(ROOT)).thenReturn(true);
        when(mockResource.getPath()).thenReturn("/");

        final Resource graphSubject = mockSubjects.reverse().convert(mockResource);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_TRANSACTION_SERVICE));
    }

    @Test
    public void shouldDecorateNodesWithLinksToVersionsAndExport() {

        when(mockResource.isVersioned()).thenReturn(true);
        when(mockResource.getPath()).thenReturn("/some/path/to/object");

        when(mockSerializers.keySet()).thenReturn(of("a", "b"));
        final Resource graphSubject = mockSubjects.reverse().convert(mockResource);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_VERSION_HISTORY));
        assertEquals(2, model.listObjectsOfProperty(graphSubject,
                HAS_SERIALIZATION).toSet().size());
    }

    @Test
    public void shouldNotDecorateNodesWithLinksToVersionsUnlessVersionable() {

        when(mockResource.isVersioned()).thenReturn(false);
        when(mockResource.getPath()).thenReturn("/some/path/to/object");

        when(mockSerializers.keySet()).thenReturn(of("a", "b"));
        final Resource graphSubject = mockSubjects.reverse().convert(mockResource);

        final Model model =
                testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertFalse(model.contains(graphSubject, HAS_VERSION_HISTORY));
    }

    @Test
    public void shouldDecorateDatastreamsWithLinksToFixityChecks() {
        when(mockBinary.getPath()).thenReturn("/some/path/to/datastream");
        when(mockSerializers.keySet()).thenReturn(new HashSet<String>());
        final Resource graphSubject = mockSubjects.reverse().convert(mockBinary);

        final Model model =
            testObj.createModelForResource(mockBinary, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_FIXITY_SERVICE));
    }

    @Test
    public void shouldDecorateRootNodeWithCorrectResourceURI() {
        when(mockResource.hasType(ROOT)).thenReturn(true);
        when(mockSerializers.keySet()).thenReturn(of("a"));
        when(mockResource.getPath()).thenReturn("/");

        final Resource graphSubject = mockSubjects.reverse().convert(mockResource);
        final Model model =
                testObj.createModelForResource(mockResource, uriInfo,
                        mockSubjects);
        assertEquals("http://localhost/fcrepo/fcr:export?format=a", model
                .getProperty(graphSubject, HAS_SERIALIZATION).getResource()
                .getURI());
    }

    @Test
    public void shouldDecorateOtherNodesWithCorrectResourceURI() {
        when(mockSerializers.keySet()).thenReturn(of("a"));
        when(mockResource.getPath()).thenReturn("/some/path/to/object");

        final Resource graphSubject = mockSubjects.reverse().convert(mockResource);
        final Model model =
                testObj.createModelForResource(mockResource, uriInfo,
                        mockSubjects);
        assertEquals(
                "http://localhost/fcrepo/some/path/to/object/fcr:export?format=a",
                model.getProperty(graphSubject, HAS_SERIALIZATION)
                        .getResource().getURI());
    }

}
