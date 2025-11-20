/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.impl;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidConditionExpressionException;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.impl.utils.SearchTestConfiguration;
import org.fcrepo.search.impl.utils.TestTransaction;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * Test class for {@link DbSearchIndexImpl}
 *
 * @author whikloj
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration(classes = {SearchTestConfiguration.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class DbSearchIndexImplTest {

    @Inject
    private DbSearchIndexImpl searchIndex;

    private ResourceHeaders resourceHeaders1;

    private ResourceHeaders resource1ParentHeaders;

    private ResourceHeaders resourceHeaders2;

    private Transaction transaction;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private FedoraResource resource1;

    @Mock
    private FedoraResource resource1Parent;

    @Mock
    private FedoraResource resource2;

    private FedoraId testId;

    private FedoraId parentId;

    @BeforeEach
    @FlywayTest
    public void setUp() throws PathNotFoundException {
        searchIndex.reset();
        testId = FedoraId.create("info:fedora/parentId/testId");
        parentId = FedoraId.create("info:fedora/parentId");
        transaction = makeTransaction();
        resourceHeaders1 = buildContainerResourceHeaders(testId, parentId);
        mockContainerResource(resource1, testId.getResourceId());
        when(resourceFactory.getResource(transaction, resourceHeaders1.getId())).thenReturn(resource1);
        setField(searchIndex, "resourceFactory", resourceFactory);
    }

    /**
     * Builds a container resource headers object.
     *
     * @param id The resource's id
     * @param parentId The resource's parent id
     * @return The resource headers object
     */
    private ResourceHeaders buildContainerResourceHeaders(final FedoraId id, final FedoraId parentId) {
        final var tempHeaders = new ResourceHeadersImpl();
        tempHeaders.setId(id);
        tempHeaders.setParent(parentId);
        tempHeaders.setStateToken("stateToken");
        tempHeaders.setInteractionModel("http://www.w3.org/ns/ldp#BasicContainer");
        tempHeaders.setCreatedDate(Instant.now());
        tempHeaders.setCreatedBy("createdBy");
        tempHeaders.setLastModifiedDate(Instant.now());
        tempHeaders.setLastModifiedBy("lastModifiedBy");
        tempHeaders.setObjectRoot(true);
        tempHeaders.setDeleted(false);
        tempHeaders.setContentPath("fcr-container.nt");
        return tempHeaders;
    }

    /**
     * Mocks some container resource properties.
     *
     * @param resource The mock resource
     * @param id The resource's id
     */
    private void mockContainerResource(final FedoraResource resource, final String id) {
        when(resource.getId()).thenReturn(id);
        when(resource.getTypes()).thenReturn(List.of(
                URI.create(BASIC_CONTAINER.getURI()),
                URI.create(FEDORA_CONTAINER.getURI()),
                URI.create(FEDORA_RESOURCE.getURI()),
                URI.create(RDF_SOURCE.getURI()),
                URI.create(RESOURCE.getURI())
        ));
    }

    /**
     * Builds a binary resource headers object.
     *
     * @param id The resource's id
     * @param parentId The resource's parent id
     * @return The resource headers object
     */
    private ResourceHeaders buildBinaryResourceHeaders(final FedoraId id, final FedoraId parentId) {
        final var tempHeaders = new ResourceHeadersImpl();
        tempHeaders.setId(id);
        tempHeaders.setParent(parentId);
        tempHeaders.setStateToken("stateToken");
        tempHeaders.setInteractionModel("http://www.w3.org/ns/ldp#NonRDFSource");
        tempHeaders.setMimeType("text/plain");
        tempHeaders.setFilename("filename.txt");
        tempHeaders.setContentSize(12345L);
        tempHeaders.setDigests(
                Collections.singleton(
                        URI.create("urn:sha-512:3395169e41043ec16a5a98f3716a826937e6d98a1667c8c5aa5a7e9be27958427541c" +
                                "944a1a2a5143ddd37fdb8360b08d80094d13c0a02e0bd83308a7b50b464")
            )
        );
        tempHeaders.setCreatedDate(Instant.now());
        tempHeaders.setCreatedBy("createdBy");
        tempHeaders.setLastModifiedDate(Instant.now());
        tempHeaders.setLastModifiedBy("lastModifiedBy");
        tempHeaders.setObjectRoot(true);
        tempHeaders.setDeleted(false);
        tempHeaders.setContentPath("some-binary");
        return tempHeaders;
    }

    /**
     * Mocks some binary resource properties.
     *
     * @param resource The mock resource
     * @param id The resource's id
     */
    private void mockBinaryResource(final FedoraResource resource, final String id) {
        when(resource.getId()).thenReturn(id);
        when(resource.getTypes()).thenReturn(List.of(
                URI.create(NON_RDF_SOURCE.getURI()),
                URI.create(FEDORA_BINARY.getURI()),
                URI.create(FEDORA_RESOURCE.getURI()),
                URI.create(RESOURCE.getURI())
        ));
    }

    /**
     * Creates a transaction object.
     * @param longLived true if the transaction is long-lived, false otherwise
     * @return The transaction object
     */
    private Transaction makeTransaction(final boolean longLived) {
        return new TestTransaction(UUID.randomUUID().toString(), !longLived);
    }

    /**
     * Creates a transaction object.
     * @return The transaction object
     */
    private Transaction makeTransaction() {
        return makeTransaction(false);
    }

    /**
     * Test with an invalid condition ("written")
     */
    @Test
    public void testInvalidCondition() throws Exception {
        assertThrows(InvalidConditionExpressionException.class, () -> {
            new SearchParameters(
                    List.of(Condition.Field.CREATED),
                    List.of(Condition.fromExpression("written=2020-01-01T00:00:00+0000")),
                    10,
                    0,
                    Condition.Field.CREATED,
                    "asc",
                    true
            );
        });
    }

    /**
     * Test with an invalid date format, must be RFC-8601 (2020-05-01T20:05:14+00:00)
     */
    @Test
    public void testInvalidDateFormat() throws Exception {
        final var parameters = new SearchParameters(
                List.of(Condition.Field.CREATED),
                List.of(Condition.fromExpression("created=2020-01-01T00:00:00Z+0000")),
                10,
                0,
                Condition.Field.CREATED,
                "asc",
                true
        );
        assertThrows(InvalidQueryException.class, () -> searchIndex.doSearch(parameters));
    }

    /**
     * Test adding a resource to the index with a short lived transaction.
     */
    @Test
    public void testAddToIndex() throws Exception {
        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=" + testId.getFullId())),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        // Search for the resource before adding it to the index
        final var results = searchIndex.doSearch(parameters);
        assertEquals(0, results.getPagination().getTotalResults());
        // Add the resource to the index
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        // Search for the resource after adding it to the index
        final var results2 = searchIndex.doSearch(parameters);
        assertEquals(1, results2.getPagination().getTotalResults());
        // Add another resource to the index
        final var id2 = FedoraId.create(UUID.randomUUID().toString());
        resourceHeaders2 = buildBinaryResourceHeaders(id2, parentId);
        mockBinaryResource(resource2, id2.getResourceId());
        when(resourceFactory.getResource(transaction, resourceHeaders2.getId())).thenReturn(resource2);
        searchIndex.addUpdateIndex(transaction, resourceHeaders2);
        // Search for the original resource
        final var results3 = searchIndex.doSearch(parameters);
        assertEquals(1, results3.getPagination().getTotalResults());
        // Search for the new resource
        final var parameters2 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=" + id2.getFullId())),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results4 = searchIndex.doSearch(parameters2);
        assertEquals(1, results4.getPagination().getTotalResults());
        // Search for both resources
        final var parameters3 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results5 = searchIndex.doSearch(parameters3);
        assertEquals(2, results5.getPagination().getTotalResults());
    }

    /**
     * Test removing a resource from the index using a short lived transaction.
     */
    @Test
    public void testRemoveFromIndex() throws Exception {
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        final var id2 = FedoraId.create(UUID.randomUUID().toString());
        resourceHeaders2 = buildBinaryResourceHeaders(id2, parentId);
        mockBinaryResource(resource2, id2.getResourceId());
        when(resourceFactory.getResource(transaction, resourceHeaders2.getId())).thenReturn(resource2);
        searchIndex.addUpdateIndex(transaction, resourceHeaders2);

        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        assertEquals(2, results.getPagination().getTotalResults());
        // Remove the first resource from the index
        searchIndex.removeFromIndex(transaction, testId);
        // Search for the resource after removing it from the index
        final var results2 = searchIndex.doSearch(parameters);
        assertEquals(1, results2.getPagination().getTotalResults());
    }

    /**
     * Test adding a resource to the index using a long lived transaction.
     */
    @Test
    public void testAddToIndexTransaction() throws Exception {
        transaction = makeTransaction(true);
        when(resourceFactory.getResource(transaction, resourceHeaders1.getId())).thenReturn(resource1);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=" + testId.getFullId())),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        // Search for the resource before adding it to the index
        final var results = searchIndex.doSearch(parameters);
        assertEquals(0, results.getPagination().getTotalResults());
        // Add the resource to the index using the transaction
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        // Search for the resource after adding it to the index
        final var results2 = searchIndex.doSearch(parameters);
        assertEquals(0, results2.getPagination().getTotalResults());
        // Commit the transaction
        searchIndex.commitTransaction(transaction);
        final var results3 = searchIndex.doSearch(parameters);
        assertEquals(1, results3.getPagination().getTotalResults());
    }

    /**
     * Test adding a resource with a long lived transaction and then rolling back the transaction.
     */
    @Test
    public void testAddToIndexTransactionRollback() throws Exception {
        transaction = makeTransaction(true);
        when(resourceFactory.getResource(transaction, resourceHeaders1.getId())).thenReturn(resource1);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=" + testId.getFullId())),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        // Search for the resource before adding it to the index
        final var results = searchIndex.doSearch(parameters);
        assertEquals(0, results.getPagination().getTotalResults());
        // Add the resource to the index using the transaction
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        // Search for the resource after adding it to the index
        final var results2 = searchIndex.doSearch(parameters);
        assertEquals(0, results2.getPagination().getTotalResults());
        // Commit the transaction
        searchIndex.rollbackTransaction(transaction);
        final var results3 = searchIndex.doSearch(parameters);
        assertEquals(0, results3.getPagination().getTotalResults());
    }

    /**
     * Test removing a resource from the index using a long lived transaction.
     */
    @Test
    public void testAddAndRemoveFromIndexTransaction() throws Exception {
        transaction = makeTransaction(true);
        when(resourceFactory.getResource(transaction, resourceHeaders1.getId())).thenReturn(resource1);
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        final var id2 = FedoraId.create(UUID.randomUUID().toString());
        resourceHeaders2 = buildBinaryResourceHeaders(id2, parentId);
        mockBinaryResource(resource2, id2.getResourceId());
        when(resourceFactory.getResource(transaction, resourceHeaders2.getId())).thenReturn(resource2);
        searchIndex.addUpdateIndex(transaction, resourceHeaders2);

        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        assertEquals(0, results.getPagination().getTotalResults());
        // Commit the transaction
        searchIndex.commitTransaction(transaction);
        // Check for both resources
        final var results2 = searchIndex.doSearch(parameters);
        assertEquals(2, results2.getPagination().getTotalResults());
        // Create a new transaction
        transaction = makeTransaction(true);
        // Update mock when the transaction changes.
        when(resourceFactory.getResource(transaction, resourceHeaders1.getId())).thenReturn(resource1);
        // Remove the first resource from the index
        searchIndex.removeFromIndex(transaction, testId);
        // Search for the resource after removing it from the index
        final var results3 = searchIndex.doSearch(parameters);
        assertEquals(2, results2.getPagination().getTotalResults());
        // Commit the transaction
        searchIndex.commitTransaction(transaction);
        final var results4 = searchIndex.doSearch(parameters);
        assertEquals(1, results4.getPagination().getTotalResults());
    }

    /**
     * Test searching for a resource by Fedora ID with and without a wildcard.
     */
    @Test
    public void testSearchFedoraId() throws Exception {
        final var parentResourceHeaders = buildContainerResourceHeaders(parentId, FedoraId.create(""));
        mockContainerResource(resource1Parent, parentId.getResourceId());
        when(resourceFactory.getResource(transaction, parentResourceHeaders.getId())).thenReturn(resource1Parent);
        searchIndex.addUpdateIndex(transaction, parentResourceHeaders);
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=" + testId.getFullId())),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        assertEquals(1, results.getPagination().getTotalResults());
        final var parameters2 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=" + parentId.getFullId())),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results2 = searchIndex.doSearch(parameters2);
        assertEquals(1, results2.getPagination().getTotalResults());
        final var parameters3 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("fedora_id=" + parentId.getFullId() + "*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results3 = searchIndex.doSearch(parameters3);
        assertEquals(2, results3.getPagination().getTotalResults());
    }

    /**
     * Test searching for a resource by rdf_type
     */
    @Test
    public void testSearchRdfType() throws Exception {
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("rdf_type=http://www.w3.org/ns/ldp#RDFSource")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        assertEquals(1, results.getPagination().getTotalResults());
        final var parameters2 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("rdf_type=http://www.w3.org/ns/ldp#NonRDFSource")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results2 = searchIndex.doSearch(parameters2);
        assertEquals(0, results2.getPagination().getTotalResults());
    }

    @Test
    public void testSearchRdfTypeWithWildcards() throws Exception {
        when(resource1.getTypes()).thenReturn(List.of(
                URI.create(BASIC_CONTAINER.getURI()),
                URI.create(FEDORA_CONTAINER.getURI()),
                URI.create(FEDORA_RESOURCE.getURI()),
                URI.create(RDF_SOURCE.getURI()),
                URI.create(RESOURCE.getURI()),
                URI.create("http://example.org/type/test_type1"),
                URI.create("http://example.org/type/test%25type3")
        ));

        final var id2 = parentId.resolve(UUID.randomUUID().toString());
        resourceHeaders2 = buildBinaryResourceHeaders(id2, parentId);
        mockBinaryResource(resource2, id2.getResourceId());
        when(resourceFactory.getResource(transaction, resourceHeaders2.getId())).thenReturn(resource2);
        when(resource2.getTypes()).thenReturn(List.of(
                URI.create(BASIC_CONTAINER.getURI()),
                URI.create(FEDORA_CONTAINER.getURI()),
                URI.create(FEDORA_RESOURCE.getURI()),
                URI.create(RDF_SOURCE.getURI()),
                URI.create(RESOURCE.getURI()),
                URI.create("http://example.org/type/testntype2")
        ));

        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        searchIndex.addUpdateIndex(transaction, resourceHeaders2);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.RDF_TYPE),
                List.of(Condition.fromExpression("rdf_type=http://example.org/type/test*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        assertEquals(3, results.getPagination().getTotalResults());

        // Ensure that underscores are not treated as wildcards
        final var parameters2 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("rdf_type=http://example.org/type/test_*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results2 = searchIndex.doSearch(parameters2);
        assertEquals(1, results2.getPagination().getTotalResults());

        // Ensure that percent signs are escaped and can be searched for
        final var parameters3 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("rdf_type=http://example.org/type/test%*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results3 = searchIndex.doSearch(parameters3);
        assertEquals(1, results3.getPagination().getTotalResults());
    }

    /**
     * Test searching for a resource by mime_type
     */
    @Test
    public void testSearchMimeType() throws Exception {
        final var id2 = parentId.resolve(UUID.randomUUID().toString());
        resourceHeaders2 = buildBinaryResourceHeaders(id2, parentId);
        mockBinaryResource(resource2, id2.getResourceId());
        when(resourceFactory.getResource(transaction, resourceHeaders2.getId())).thenReturn(resource2);
        searchIndex.addUpdateIndex(transaction, resourceHeaders2);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("mime_type=text/plain")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        assertEquals(1, results.getPagination().getTotalResults());

        final var parameters2 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("mime_type=application/json")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results2 = searchIndex.doSearch(parameters2);
        assertEquals(0, results2.getPagination().getTotalResults());

        // Verify wildcard searching
        final var parameters3 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("mime_type=text/pl*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results3 = searchIndex.doSearch(parameters3);
        assertEquals(1, results3.getPagination().getTotalResults());

        // Verify that underscores are not treated as wildcards
        final var parameters4 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("mime_type=text/pl_*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results4 = searchIndex.doSearch(parameters4);
        assertEquals(0, results4.getPagination().getTotalResults());

        // Verify that percents are not treated as wildcards
        final var parameters5 = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID),
                List.of(Condition.fromExpression("mime_type=text/pl%*")),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results5 = searchIndex.doSearch(parameters5);
        assertEquals(0, results5.getPagination().getTotalResults());
    }

    /**
     * Test searching for a resource by created date
     */
    @Test
    public void testSearchCreated() throws Exception {
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.CREATED),
                List.of(Condition.fromExpression("created=2020-01-01T00:00:00+00:00")),
                10,
                0,
                Condition.Field.CREATED,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        // Created does not exact match
        assertEquals(0, results.getPagination().getTotalResults());

        final var parameters2 = new SearchParameters(
                List.of(Condition.Field.CREATED),
                List.of(Condition.fromExpression("created=" + resourceHeaders1.getCreatedDate().toString())),
                10,
                0,
                Condition.Field.CREATED,
                "asc",
                true
        );
        final var results2 = searchIndex.doSearch(parameters2);
        // Created does exact match
        assertEquals(1, results2.getPagination().getTotalResults());

        final var parameters3 = new SearchParameters(
                List.of(Condition.Field.CREATED),
                List.of(Condition.fromExpression("created<=2025-01-01T00:00:00+00:00")),
                10,
                0,
                Condition.Field.CREATED,
                "asc",
                true
        );
        final var results3 = searchIndex.doSearch(parameters3);
        // Created is not less than or equal to 2025-01-01
        assertEquals(0, results3.getPagination().getTotalResults());

        final var parameters4 = new SearchParameters(
                List.of(Condition.Field.CREATED),
                List.of(Condition.fromExpression("created>=" + resourceHeaders1.getCreatedDate().toString())),
                10,
                0,
                Condition.Field.CREATED,
                "asc",
                true
        );
        final var results4 = searchIndex.doSearch(parameters4);
        // Created is greater than or equal to the created date
        assertEquals(1, results4.getPagination().getTotalResults());
    }

    /**
     * Test searching for a resource by content size
     */
    @Test
    public void testSearchContentSize() throws Exception {
        final var id2 = parentId.resolve(UUID.randomUUID().toString());
        resourceHeaders2 = buildBinaryResourceHeaders(id2, parentId);
        mockBinaryResource(resource2, id2.getResourceId());
        when(resourceFactory.getResource(transaction, resourceHeaders2.getId())).thenReturn(resource2);
        searchIndex.addUpdateIndex(transaction, resourceHeaders2);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.CONTENT_SIZE),
                List.of(Condition.fromExpression("content_size=12345")),
                10,
                0,
                Condition.Field.CONTENT_SIZE,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        assertEquals(1, results.getPagination().getTotalResults());
        final var parameters2 = new SearchParameters(
                List.of(Condition.Field.CONTENT_SIZE),
                List.of(Condition.fromExpression("content_size=1234")),
                10,
                0,
                Condition.Field.CONTENT_SIZE,
                "asc",
                true
        );
        final var results2 = searchIndex.doSearch(parameters2);
        assertEquals(0, results2.getPagination().getTotalResults());
    }

    /**
     * Test searching for a resource by multiple conditions
     */
    @Test
    public void testComplexQuery() throws Exception {
        final var id2 = parentId.resolve(UUID.randomUUID().toString());
        resourceHeaders2 = buildBinaryResourceHeaders(id2, parentId);
        mockBinaryResource(resource2, id2.getResourceId());
        when(resourceFactory.getResource(transaction, resourceHeaders2.getId())).thenReturn(resource2);
        searchIndex.addUpdateIndex(transaction, resourceHeaders1);
        searchIndex.addUpdateIndex(transaction, resourceHeaders2);
        final var parameters = new SearchParameters(
                List.of(Condition.Field.FEDORA_ID, Condition.Field.RDF_TYPE),
                List.of(
                        Condition.fromExpression("fedora_id=" + parentId.getFullId() + "*"),
                        Condition.fromExpression("created>=2025-01-01T00:00:00+00:00")
                ),
                10,
                0,
                Condition.Field.FEDORA_ID,
                "asc",
                true
        );
        final var results = searchIndex.doSearch(parameters);
        assertEquals(2, results.getPagination().getTotalResults());
    }
}
