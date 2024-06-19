/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static org.apache.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_OCFL_PATH;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.config.DisplayOcflPath;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author whikloj
 */
public class ManagedPropertiesServiceImplTest {

    @Mock
    private Container containerResource;

    @Mock
    private Binary binaryResource;

    private static final String RESOURCE_ID = "info:fedora/resource1";

    private static final String RESOURCE_HASH = "69bb8862c69cfa4c1a607c9a8afa693bbd714353f29327a40b0c671d532035f9";
    private static final String RESOURCE_OCFL_PATH = RESOURCE_HASH.substring(0,3) + "/" +
            RESOURCE_HASH.substring(3, 6) + "/" + RESOURCE_HASH.substring(6, 9) + "/" + RESOURCE_HASH;

    private static final String OCFL_ROOT = "/ocfl/root";

    private static final FedoraId FEDORA_ID = FedoraId.create(RESOURCE_ID);

    private final Resource subject = createResource(FEDORA_ID.getFullId());

    private static final String USER = "test";

    private static final Instant RESOURCE_LAST_MODIFIED_DATE = Instant.now().minus(2, ChronoUnit.HOURS);

    private static final Instant RESOURCE_CREATED_DATE = Instant.now().minus(1, ChronoUnit.DAYS);

    @Mock
    private OcflPropsConfig ocflPropsConfig;

    @InjectMocks
    private ManagedPropertiesServiceImpl managedPropertiesService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(ocflPropsConfig.getDisplayOcflPath()).thenReturn(DisplayOcflPath.NONE);
        when(ocflPropsConfig.getOcflRepoRoot()).thenReturn(Path.of(OCFL_ROOT));

        when(containerResource.getDescribedResource()).thenReturn(containerResource);
        when(containerResource.getOriginalResource()).thenReturn(containerResource);
        when(containerResource.getCreatedBy()).thenReturn(USER);
        when(containerResource.getCreatedDate()).thenReturn(RESOURCE_CREATED_DATE);
        when(containerResource.getLastModifiedBy()).thenReturn(USER);
        when(containerResource.getLastModifiedDate()).thenReturn(RESOURCE_LAST_MODIFIED_DATE);
        when(containerResource.getFedoraId()).thenReturn(FEDORA_ID);
        when(containerResource.getId()).thenReturn(FEDORA_ID.getResourceId());

        when(binaryResource.getDescribedResource()).thenReturn(binaryResource);
        when(binaryResource.getOriginalResource()).thenReturn(binaryResource);
        when(binaryResource.getCreatedBy()).thenReturn(USER);
        when(binaryResource.getCreatedDate()).thenReturn(RESOURCE_CREATED_DATE);
        when(binaryResource.getLastModifiedBy()).thenReturn(USER);
        when(binaryResource.getLastModifiedDate()).thenReturn(RESOURCE_LAST_MODIFIED_DATE);
        when(binaryResource.getFedoraId()).thenReturn(FEDORA_ID);
        when(binaryResource.getId()).thenReturn(FEDORA_ID.getResourceId());
        when(binaryResource.getContentSize()).thenReturn(1234L);
        when(binaryResource.getFilename()).thenReturn("test.txt");
        when(binaryResource.getMimeType()).thenReturn("text/plain");
        when(binaryResource.getContentDigests()).thenReturn(Collections.singleton(URI.create("sha1:1234")));
    }

    /**
     * Assert the expected model for a container resource.
     * @param model the model to check
     */
    private void assertExpectedContainerModel(final Model model) {
        assertTrue(model.contains(
                subject,
                CREATED_DATE,
                createTypedLiteral(RESOURCE_CREATED_DATE.toString(), XSDdateTime)
        ));
        assertTrue(model.contains(
                subject,
                CREATED_BY,
                USER
        ));
        assertTrue(model.contains(
                subject,
                LAST_MODIFIED_DATE,
                createTypedLiteral(RESOURCE_LAST_MODIFIED_DATE.toString(), XSDdateTime)
        ));
        assertTrue(model.contains(
                subject,
                LAST_MODIFIED_BY,
                USER
        ));
    }

    /**
     * Assert the expected model for a binary resource.
     * @param model the model to check
     */
    private void assertExpectedBinaryModel(final Model model) {
        assertExpectedContainerModel(model);
        assertTrue(model.contains(
                subject,
                HAS_SIZE,
                createTypedLiteral("1234", XSDlong)
        ));
        assertTrue(model.contains(
                subject,
                HAS_ORIGINAL_NAME,
                createPlainLiteral("test.txt")
        ));
        assertTrue(model.contains(
                subject,
                HAS_MIME_TYPE,
                createPlainLiteral("text/plain")
        ));
        assertTrue(model.contains(
                subject,
                HAS_MESSAGE_DIGEST,
                createResource("sha1:1234")
        ));
    }

    private void assertTripleExistsAndMatches(final Model model, final Resource subject, final Property predicate,
            final RDFNode object) {
        assertTrue(model.contains(subject, predicate, object));
        final var stmts = model.listObjectsOfProperty(subject, predicate);
        while (stmts.hasNext()) {
            final var stmt = stmts.next();
            assertEquals(object, stmt);
        }
    }

    /**
     * Test a normal container without an OCFL path but all the other SMTs.
     */
    @Test
    public void testGetContainer() {
        final var triples = managedPropertiesService.get(containerResource);
        final var model = triples.collect(toModel());
        assertExpectedContainerModel(model);
        assertFalse(model.contains(
                subject,
                FEDORA_OCFL_PATH,
                (RDFNode) null
        ));
    }

    @Test
    public void testGetContainerRelative() {
        when(ocflPropsConfig.getDisplayOcflPath()).thenReturn(DisplayOcflPath.RELATIVE);
        final var triples = managedPropertiesService.get(containerResource);
        final var model = triples.collect(toModel());
        assertExpectedContainerModel(model);
        assertTrue(model.contains(
                subject,
                FEDORA_OCFL_PATH,
                RESOURCE_OCFL_PATH
        ));
    }

    @Test
    public void testGetContainerAbsolute() {
        when(ocflPropsConfig.getDisplayOcflPath()).thenReturn(DisplayOcflPath.ABSOLUTE);
        final var triples = managedPropertiesService.get(containerResource);
        final var model = triples.collect(toModel());
        assertExpectedContainerModel(model);
        assertTripleExistsAndMatches(
                model,
                subject,
                FEDORA_OCFL_PATH,
                createPlainLiteral(OCFL_ROOT + "/" + RESOURCE_OCFL_PATH)
        );
    }

    @Test
    public void testGetBinary() {
        final var triples = managedPropertiesService.get(binaryResource);
        final var model = triples.collect(toModel());
        assertExpectedBinaryModel(model);
        assertFalse(model.contains(
                subject,
                FEDORA_OCFL_PATH,
                (RDFNode) null
        ));
    }

    @Test
    public void testGetBinaryRelative() {
        when(ocflPropsConfig.getDisplayOcflPath()).thenReturn(DisplayOcflPath.RELATIVE);
        final var triples = managedPropertiesService.get(binaryResource);
        final var model = triples.collect(toModel());
        assertExpectedBinaryModel(model);
        assertTrue(model.contains(
                subject,
                FEDORA_OCFL_PATH,
                RESOURCE_OCFL_PATH
        ));
    }

    @Test
    public void testGetBinaryAbsolute() {
        when(ocflPropsConfig.getDisplayOcflPath()).thenReturn(DisplayOcflPath.ABSOLUTE);
        final var triples = managedPropertiesService.get(binaryResource);
        final var model = triples.collect(toModel());
        assertExpectedBinaryModel(model);
        assertTripleExistsAndMatches(
                model,
                subject,
                FEDORA_OCFL_PATH,
                createPlainLiteral(OCFL_ROOT + "/" + RESOURCE_OCFL_PATH)
        );
    }
}
