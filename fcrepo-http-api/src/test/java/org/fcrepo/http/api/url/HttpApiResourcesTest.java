/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
