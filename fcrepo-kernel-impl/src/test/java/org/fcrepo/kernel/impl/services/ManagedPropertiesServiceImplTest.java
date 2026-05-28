/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PARENT;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.models.WebacAcl;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the ManagedPropertiesServiceImpl
 * @author whikloj
 */
public class ManagedPropertiesServiceImplTest {

    ManagedPropertiesServiceImpl managedPropertiesServiceImpl = new ManagedPropertiesServiceImpl();
    FedoraResource resource = mock(FedoraResource.class);
    FedoraResource parent = mock(FedoraResource.class);
    final Instant created_date = Instant.now();

    @BeforeEach
    public void setUp() {
        configureResource(resource);
        when(parent.getId()).thenReturn("info:fedora/parent");
        when(parent.getFedoraId()).thenReturn(FedoraId.create("parent"));
    }

    private void configureResource(final FedoraResource resource) {
        when(resource.getFedoraId()).thenReturn(FedoraId.create("resource"));
        when(resource.getId()).thenReturn("info:fedora/resource");
        when(resource.getDescribedResource()).thenReturn(resource);
        when(resource.getCreatedBy()).thenReturn("createdBy");
        when(resource.getCreatedDate()).thenReturn(created_date);
        when(resource.getLastModifiedBy()).thenReturn("lastModifiedBy");
        when(resource.getLastModifiedDate()).thenReturn(created_date);
    }

    @Test
    public void testGetNoParent() throws PathNotFoundException {
        when(resource.getParent()).thenThrow(PathNotFoundException.class);
        final Stream<Triple> result = managedPropertiesServiceImpl.get(resource);
        assertNotNull(result);
        final var model = result.collect(toModel());
        assertTrue(model.contains(null, CREATED_BY, model.createLiteral("createdBy")));
        assertTrue(model.contains(null, CREATED_DATE, model.createTypedLiteral(created_date.toString(), XSDdateTime)));
        assertTrue(model.contains(null, LAST_MODIFIED_BY, model.createLiteral("lastModifiedBy")));
        assertTrue(model.contains(
                null,
                LAST_MODIFIED_DATE,
                model.createTypedLiteral(created_date.toString(), XSDdateTime)
        ));
        assertFalse(model.contains(null, HAS_PARENT, (RDFNode) null));
    }

    @Test
    public void testGetParent() throws PathNotFoundException {
        when(resource.getParent()).thenReturn(parent);
        final Stream<Triple> result = managedPropertiesServiceImpl.get(resource);
        assertNotNull(result);
        final var model = result.collect(toModel());
        assertTrue(model.contains(null, CREATED_BY, model.createLiteral("createdBy")));
        assertTrue(model.contains(null, CREATED_DATE, model.createTypedLiteral(created_date.toString(), XSDdateTime)));
        assertTrue(model.contains(null, LAST_MODIFIED_BY, model.createLiteral("lastModifiedBy")));
        assertTrue(model.contains(
                null,
                LAST_MODIFIED_DATE,
                model.createTypedLiteral(created_date.toString(), XSDdateTime)
        ));
        assertTrue(model.contains(null, HAS_PARENT, model.createResource(URI.create("info:fedora/parent").toString())));
    }

    @Test
    public void testGetParentBinary() throws PathNotFoundException {
        final Binary binary = mock(Binary.class);
        configureResource(binary);
        when(binary.getParent()).thenReturn(parent);
        when(resource.getId()).thenReturn("info:fedora/resource/fcr:metadata");
        when(resource.getDescribedResource()).thenReturn(binary);
        final Stream<Triple> result = managedPropertiesServiceImpl.get(resource);
        assertNotNull(result);
        final var model = result.collect(toModel());
        assertTrue(model.contains(null, CREATED_BY, model.createLiteral("createdBy")));
        assertTrue(model.contains(null, CREATED_DATE, model.createTypedLiteral(created_date.toString(), XSDdateTime)));
        assertTrue(model.contains(null, LAST_MODIFIED_BY, model.createLiteral("lastModifiedBy")));
        assertTrue(model.contains(
                null,
                LAST_MODIFIED_DATE,
                model.createTypedLiteral(created_date.toString(), XSDdateTime)
        ));
        assertTrue(model.contains(null, HAS_PARENT, model.createResource(URI.create("info:fedora/parent").toString())));
    }

    @Test
    public void testGetParentAcl() {
        resource = mock(WebacAcl.class);
        configureResource(resource);
        when(resource.isAcl()).thenReturn(true);
        final Stream<Triple> result = managedPropertiesServiceImpl.get(resource);
        assertNotNull(result);
        final var model = result.collect(toModel());
        assertTrue(model.contains(null, CREATED_BY, model.createLiteral("createdBy")));
        assertTrue(model.contains(null, CREATED_DATE, model.createTypedLiteral(created_date.toString(), XSDdateTime)));
        assertTrue(model.contains(null, LAST_MODIFIED_BY, model.createLiteral("lastModifiedBy")));
        assertTrue(model.contains(
                null,
                LAST_MODIFIED_DATE,
                model.createTypedLiteral(created_date.toString(), XSDdateTime)
        ));
        assertFalse(model.contains(
                null,
                HAS_PARENT,
                model.createResource(URI.create("info:fedora/parent").toString())
        ));
    }

    @Test
    public void testGetParentTimeMap() {
        resource = mock(TimeMap.class);
        configureResource(resource);
        final Stream<Triple> result = managedPropertiesServiceImpl.get(resource);
        assertNotNull(result);
        final var model = result.collect(toModel());
        assertTrue(model.contains(null, CREATED_BY, model.createLiteral("createdBy")));
        assertTrue(model.contains(null, CREATED_DATE, model.createTypedLiteral(created_date.toString(), XSDdateTime)));
        assertTrue(model.contains(null, LAST_MODIFIED_BY, model.createLiteral("lastModifiedBy")));
        assertTrue(model.contains(
                null,
                LAST_MODIFIED_DATE,
                model.createTypedLiteral(created_date.toString(), XSDdateTime)
        ));
        assertFalse(model.contains(
                null,
                HAS_PARENT,
                model.createResource(URI.create("info:fedora/parent").toString())
        ));
    }

    @Test
    public void testGetParentTombstone() throws PathNotFoundException {
        final FedoraResource holder = mock(FedoraResource.class);
        final Tombstone tombstone = mock(Tombstone.class);
        when(holder.getId()).thenReturn("info:fedora/resource");
        when(holder.getFedoraId()).thenReturn(FedoraId.create("resource"));
        when(holder.getDescribedResource()).thenReturn(holder);
        when(holder.getOriginalResource()).thenReturn(tombstone);
        when(tombstone.getId()).thenReturn("info:fedora/resource/fcr:tombstone");
        when(tombstone.getDescribedResource()).thenReturn(tombstone);
        when(tombstone.getDeletedObject()).thenReturn(resource);
        when(resource.getParent()).thenReturn(parent);;
        final Stream<Triple> result = managedPropertiesServiceImpl.get(holder);
        assertNotNull(result);
        final var model = result.collect(toModel());
        assertTrue(model.contains(null, CREATED_BY, model.createLiteral("createdBy")));
        assertTrue(model.contains(null, CREATED_DATE, model.createTypedLiteral(created_date.toString(), XSDdateTime)));
        assertTrue(model.contains(null, LAST_MODIFIED_BY, model.createLiteral("lastModifiedBy")));
        assertTrue(model.contains(
                null,
                LAST_MODIFIED_DATE,
                model.createTypedLiteral(created_date.toString(), XSDdateTime)
        ));
        assertFalse(model.contains(
                null,
                HAS_PARENT,
                model.createResource(URI.create("info:fedora/parent").toString())
        ));
    }
}
