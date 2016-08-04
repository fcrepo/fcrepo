/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_REPOSITORY_ROOT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION_HISTORY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Session;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

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
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks() {
        when(mockResource.hasType(FEDORA_REPOSITORY_ROOT)).thenReturn(true);
        final Resource graphSubject = mockSubjects.toDomain(mockResource.getPath());
        when(mockResource.asUri(mockSubjects)).thenReturn(graphSubject);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_TRANSACTION_SERVICE));
    }

    @Test
    public void shouldDecorateNodesWithLinksToVersions() {

        when(mockResource.isVersioned()).thenReturn(true);
        when(mockResource.getPath()).thenReturn("/some/path/to/object");
        final Resource graphSubject = mockSubjects.toDomain(mockResource.getPath());
        when(mockResource.asUri(mockSubjects)).thenReturn(graphSubject);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_VERSION_HISTORY));
    }

    @Test
    public void shouldNotDecorateNodesWithLinksToVersionsUnlessVersionable() {

        when(mockResource.isVersioned()).thenReturn(false);
        when(mockResource.getPath()).thenReturn("/some/path/to/object");
        final Resource graphSubject = mockSubjects.toDomain(mockResource.getPath());
        when(mockResource.asUri(mockSubjects)).thenReturn(graphSubject);

        final Model model =
                testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertFalse(model.contains(graphSubject, HAS_VERSION_HISTORY));
    }

    @Test
    public void shouldDecorateDatastreamsWithLinksToFixityChecks() {
        when(mockBinary.getPath()).thenReturn("/some/path/to/datastream");
        when(mockBinary.getDescribedResource()).thenReturn(mockBinary);
        final Resource graphSubject = mockSubjects.toDomain(mockBinary.getPath());
        when(mockBinary.asUri(mockSubjects)).thenReturn(graphSubject);

        final Model model =
            testObj.createModelForResource(mockBinary, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_FIXITY_SERVICE));
    }
}
