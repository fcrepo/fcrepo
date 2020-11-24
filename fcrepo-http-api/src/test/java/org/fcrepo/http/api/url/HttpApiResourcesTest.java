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

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_ROOT;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.UriInfo;

import java.util.UUID;

/**
 * <p>HttpApiResourcesTest class.</p>
 *
 * @author awoods
 * @author ajs6f
 * @author whikloj
 */

@RunWith(MockitoJUnitRunner.Silent.class)
public class HttpApiResourcesTest {

    private HttpApiResources testObj;

    private UriInfo uriInfo;

    @Mock
    private FedoraResource mockResource;

    @Mock
    private Binary mockBinary;

    @Mock
    private NonRdfSourceDescription mockDescription;

    private FedoraId resourceId;

    @Before
    public void setUp() {
        testObj = new HttpApiResources();
        uriInfo = getUriInfoImpl();
        resourceId = FedoraId.create(UUID.randomUUID().toString());
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks() {
        when(mockResource.hasType(REPOSITORY_ROOT.getURI())).thenReturn(true);
        when(mockResource.getFedoraId()).thenReturn(resourceId);

        final Resource graphSubject = createResource(resourceId.getFullId());

        final Model model =
            testObj.createModelForResource(mockResource, uriInfo);

        assertTrue(model.contains(graphSubject, HAS_TRANSACTION_SERVICE));
    }

    @Test
    public void shouldDecorateDescriptionWithLinksToFixityChecks() {
        final var descriptionId = resourceId.resolve(FCR_METADATA);
        when(mockDescription.getDescribedResource()).thenReturn(mockBinary);
        when(mockDescription.getFedoraId()).thenReturn(descriptionId);
        when(mockBinary.getDescribedResource()).thenReturn(mockBinary);
        when(mockBinary.getFedoraId()).thenReturn(resourceId);
        final Resource graphSubject = createResource(resourceId.getFullId());

        final Model model =
            testObj.createModelForResource(mockDescription, uriInfo);

        assertTrue(model.contains(graphSubject, HAS_FIXITY_SERVICE));
    }
}
