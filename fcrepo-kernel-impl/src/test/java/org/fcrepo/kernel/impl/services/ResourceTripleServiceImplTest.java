/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.rdf.LdpTriplePreferences;
import org.fcrepo.kernel.api.services.ContainmentTriplesService;
import org.fcrepo.kernel.api.services.ManagedPropertiesService;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Unit test for ResourceTripleServiceImpl
 *
 * @author bbpennel
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class ResourceTripleServiceImplTest {

    @Mock
    private ManagedPropertiesService managedPropertiesService;

    @Mock
    private ContainmentTriplesService containmentTriplesService;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private Transaction transaction;

    @Mock
    private FedoraResource resource;

    @Mock
    private LdpTriplePreferences preferences;

    @InjectMocks
    private ResourceTripleServiceImpl service;

    private Node resourceSubject;
    private FedoraId resourceId;

    private Triple userTriple1;
    private Triple userTriple2;
    private Triple serverTriple1;
    private Triple serverTriple2;
    private Triple containmentTriple1;
    private Triple containmentTriple2;
    private Triple membershipTriple1;
    private Triple membershipTriple2;
    private Triple referenceTriple1;
    private Triple referenceTriple2;
    private Triple membershipByObjectTriple;
    private Triple containedByTriple;

    @BeforeEach
    public void setup() {
        resourceId = FedoraId.create("info:fedora/test-resource");
        resourceSubject = createURI(resourceId.getFullId());

        when(resource.getFedoraId()).thenReturn(resourceId);

        // Create test triples for each source
        userTriple1 = new Triple(resourceSubject,
                createURI("http://purl.org/dc/elements/1.1/title"),
                createLiteral("Test Resource"));
        userTriple2 = new Triple(resourceSubject,
                createURI("http://purl.org/dc/elements/1.1/description"),
                createLiteral("A test resource for unit testing"));

        serverTriple1 = new Triple(resourceSubject,
                createURI("http://fedora.info/definitions/v4/repository#created"),
                createLiteral("2023-01-01T00:00:00Z"));
        serverTriple2 = new Triple(resourceSubject,
                createURI("http://fedora.info/definitions/v4/repository#lastModified"),
                createLiteral("2023-01-02T12:34:56Z"));

        final Node childNode = createURI("info:fedora/test-resource/child");
        containmentTriple1 = new Triple(resourceSubject,
                createURI("http://www.w3.org/ns/ldp#contains"),
                childNode);
        containmentTriple2 = new Triple(resourceSubject,
                createURI("http://www.w3.org/ns/ldp#contains"),
                createURI("info:fedora/test-resource/another-child"));

        membershipTriple1 = new Triple(resourceSubject,
                createURI("http://www.w3.org/ns/ldp#member"),
                createURI("info:fedora/test-resource/member1"));
        membershipTriple2 = new Triple(resourceSubject,
                createURI("http://www.w3.org/ns/ldp#member"),
                createURI("info:fedora/test-resource/member2"));

        referenceTriple1 = new Triple(createURI("info:fedora/other-resource"),
                createURI("http://purl.org/dc/elements/1.1/relation"),
                resourceSubject);
        referenceTriple2 = new Triple(createURI("info:fedora/other-resource2"),
                createURI("http://purl.org/dc/elements/1.1/relation"),
                resourceSubject);

        membershipByObjectTriple = new Triple(createURI("info:fedora/collection"),
                createURI("http://www.w3.org/ns/ldp#member"),
                resourceSubject);

        containedByTriple = new Triple(createURI("info:fedora/parent"),
                createURI("http://www.w3.org/ns/ldp#contains"),
                resourceSubject);

        when(resource.getTriples()).thenReturn(rdfStreamOf(userTriple1, userTriple2));
        when(managedPropertiesService.get(resource)).thenReturn(of(serverTriple1, serverTriple2));
        when(containmentTriplesService.get(transaction, resource))
                .thenReturn(of(containmentTriple1, containmentTriple2));
        when(membershipService.getMembership(transaction, resourceId))
                .thenReturn(rdfStreamOf(membershipTriple1, membershipTriple2));
        when(referenceService.getInboundReferences(transaction, resource))
                .thenReturn(rdfStreamOf(referenceTriple1, referenceTriple2));
        when(membershipService.getMembershipByObject(transaction, resourceId))
                .thenReturn(rdfStreamOf(membershipByObjectTriple));
        when(containmentTriplesService.getContainedBy(transaction, resource))
                .thenReturn(of(containedByTriple));
    }

    private RdfStream rdfStreamOf(final Triple... triples) {
        return new DefaultRdfStream(resourceSubject, of(triples));
    }

    @Test
    public void testGetResourceTriples_AllPreferences() {
        // Setup preferences to include everything
        when(preferences.displayUserRdf()).thenReturn(true);
        when(preferences.displayServerManaged()).thenReturn(true);
        when(preferences.displayContainment()).thenReturn(true);
        when(preferences.displayMembership()).thenReturn(true);
        when(preferences.displayReferences()).thenReturn(true);

        // Call the service
        final Stream<Triple> resultStream = service.getResourceTriples(transaction, resource, preferences, -1);

        // Get all triples from the stream
        final List<Triple> results = resultStream.collect(Collectors.toList());

        // Verify all triples are included
        assertEquals(12, results.size());
        assertTrue(results.contains(userTriple1));
        assertTrue(results.contains(userTriple2));
        assertTrue(results.contains(serverTriple1));
        assertTrue(results.contains(serverTriple2));
        assertTrue(results.contains(containmentTriple1));
        assertTrue(results.contains(containmentTriple2));
        assertTrue(results.contains(membershipTriple1));
        assertTrue(results.contains(membershipTriple2));
        assertTrue(results.contains(referenceTriple1));
        assertTrue(results.contains(referenceTriple2));
        assertTrue(results.contains(membershipByObjectTriple));
        assertTrue(results.contains(containedByTriple));
    }

    @Test
    public void testGetResourceTriples_UserRdfOnly() {
        // Setup preferences to include only user RDF
        when(preferences.displayUserRdf()).thenReturn(true);
        when(preferences.displayServerManaged()).thenReturn(false);
        when(preferences.displayContainment()).thenReturn(false);
        when(preferences.displayMembership()).thenReturn(false);
        when(preferences.displayReferences()).thenReturn(false);

        // Call the service
        final Stream<Triple> resultStream = service.getResourceTriples(transaction, resource, preferences, -1);

        // Get all triples from the stream
        final List<Triple> results = resultStream.collect(Collectors.toList());

        // Verify only user triples are included
        assertEquals(2, results.size());
        assertTrue(results.contains(userTriple1));
        assertTrue(results.contains(userTriple2));
    }

    @Test
    public void testGetResourceTriples_WithLimit() {
        // Setup preferences to include containment triples
        when(preferences.displayUserRdf()).thenReturn(false);
        when(preferences.displayServerManaged()).thenReturn(false);
        when(preferences.displayContainment()).thenReturn(true);
        when(preferences.displayMembership()).thenReturn(false);
        when(preferences.displayReferences()).thenReturn(false);

        // Setup containment service with triples and make sure limit is respected
        final Stream<Triple> containmentStream = of(containmentTriple1, containmentTriple2);
        when(containmentTriplesService.get(transaction, resource)).thenReturn(containmentStream);

        // Call the service with a limit of 1
        final Stream<Triple> resultStream = service.getResourceTriples(transaction, resource, preferences, 1);

        // Get all triples from the stream
        final List<Triple> results = resultStream.collect(Collectors.toList());

        // Verify only one containment triple is included due to limit
        assertEquals(1, results.size());

        // Verify containment service was called
        verify(containmentTriplesService).get(transaction, resource);
    }

    @Test
    public void testGetResourceTriples_NoTriples() {
        // Setup preferences - shouldn't matter as all services return empty
        when(preferences.displayUserRdf()).thenReturn(true);
        when(preferences.displayServerManaged()).thenReturn(true);
        when(preferences.displayContainment()).thenReturn(true);
        when(preferences.displayMembership()).thenReturn(true);
        when(preferences.displayReferences()).thenReturn(true);

        // Setup all services to return empty streams
        when(resource.getTriples()).thenReturn(rdfStreamOf());
        when(managedPropertiesService.get(resource)).thenReturn(Stream.empty());
        when(containmentTriplesService.get(transaction, resource)).thenReturn(Stream.empty());
        when(membershipService.getMembership(transaction, resourceId)).thenReturn(rdfStreamOf());
        when(referenceService.getInboundReferences(transaction, resource)).thenReturn(rdfStreamOf());
        when(membershipService.getMembershipByObject(transaction, resourceId)).thenReturn(rdfStreamOf());
        when(containmentTriplesService.getContainedBy(transaction, resource)).thenReturn(Stream.empty());

        // Call the service
        final Stream<Triple> resultStream = service.getResourceTriples(transaction, resource, preferences, -1);

        // Get all triples from the stream
        final List<Triple> results = resultStream.collect(Collectors.toList());

        // Verify no triples returned
        assertEquals(0, results.size());

        // Verify all services were called
        verify(resource).getTriples();
        verify(managedPropertiesService).get(resource);
        verify(containmentTriplesService).get(transaction, resource);
        verify(membershipService).getMembership(transaction, resourceId);
        verify(referenceService).getInboundReferences(transaction, resource);
        verify(membershipService).getMembershipByObject(transaction, resourceId);
        verify(containmentTriplesService).getContainedBy(transaction, resource);
    }
}