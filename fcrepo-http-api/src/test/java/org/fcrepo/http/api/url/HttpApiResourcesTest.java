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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * <p>HttpApiResourcesTest class.</p>
 *
 * @author awoods
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpApiResourcesTest {

    private HttpApiResources testObj;

    private UriInfo uriInfo;

    private HttpResourceConverter mockSubjects;

    @Mock
    private HttpSession mockSession;

    @Mock
    private FedoraResource mockResource;

    @Mock
    private FedoraBinary mockBinary;

    @Mock
    private NonRdfSourceDescription mockDescription;

    @Before
    public void setUp() {
        testObj = new HttpApiResources();
        uriInfo = getUriInfoImpl();
        mockSubjects = new HttpResourceConverter(mockSession, UriBuilder.fromUri("http://localhost/{path: .*}"));
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks() {
        when(mockResource.hasType(FEDORA_REPOSITORY_ROOT)).thenReturn(true);
        when(mockResource.getPath()).thenReturn("/");

        final Resource graphSubject = mockSubjects.reverse().convert(mockResource);

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_TRANSACTION_SERVICE));
    }

    @Test
    @Ignore("Until implemented with Memento")
    public void shouldDecorateNodesWithLinksToVersions() {
    }

    @Test
    @Ignore ("Until implemented with Memento")
    public void shouldNotDecorateNodesWithLinksToVersionsUnlessVersionable() {
    }

    @Test
    public void shouldDecorateDescriptionWithLinksToFixityChecks() {
        when(mockDescription.getDescribedResource()).thenReturn(mockBinary);
        when(mockDescription.getPath()).thenReturn("/some/path/to/datastream/fedora:description");
        when(mockBinary.getPath()).thenReturn("/some/path/to/datastream");
        when(mockBinary.getDescribedResource()).thenReturn(mockBinary);
        final Resource graphSubject = mockSubjects.reverse().convert(mockBinary);

        final Model model =
            testObj.createModelForResource(mockDescription, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_FIXITY_SERVICE));
    }
}
