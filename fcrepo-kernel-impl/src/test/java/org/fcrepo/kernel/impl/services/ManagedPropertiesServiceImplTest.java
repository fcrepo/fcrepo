/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PARENT;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the ManagedPropertiesServiceImpl
 * @author whikloj
 */
public class ManagedPropertiesServiceImplTest {

    ManagedPropertiesServiceImpl managedPropertiesServiceImpl = new ManagedPropertiesServiceImpl();
    FedoraResource resource = mock(FedoraResource.class);
    final Instant created_date = Instant.now();

    @Before
    public void setUp() {
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
        final FedoraResource parent = mock(FedoraResource.class);
        when(parent.getId()).thenReturn("info:fedora/parent");
        when(resource.getParent()).thenReturn(parent);
        when(parent.getFedoraId()).thenReturn(FedoraId.create("parent"));
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
}
