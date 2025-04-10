/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.stats.api.MimeTypeStatsResult;
import org.fcrepo.stats.api.RdfTypeStatsResult;
import org.fcrepo.stats.api.RepositoryStatsByMimeTypeResults;
import org.fcrepo.stats.api.RepositoryStatsByRdfTypeResults;
import org.fcrepo.stats.api.RepositoryStatsParameters;
import org.fcrepo.stats.api.RepositoryStatsResult;
import org.fcrepo.stats.impl.utils.StatsTestConfiguration;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author bbpennel
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {StatsTestConfiguration.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class})
public class DbRepositoryStatsImplTest {

    @Inject
    private DbRepositoryStatsImpl repositoryStats;

    @Inject
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private int nextId;

    @BeforeEach
    @FlywayTest
    public void setUp() {
        nextId = 1;
        jdbcTemplate = new JdbcTemplate(dataSource);
        repositoryStats.setup();

        // Clear the database
        jdbcTemplate.update("DELETE FROM simple_search");
        jdbcTemplate.update("DELETE FROM search_resource_rdf_type");
        jdbcTemplate.update("DELETE FROM search_rdf_type");

        // Insert test RDF types
        jdbcTemplate.update("INSERT INTO search_rdf_type (id, rdf_type_uri) VALUES (1, ?)",
                RdfLexicon.BASIC_CONTAINER.getURI());
        jdbcTemplate.update("INSERT INTO search_rdf_type (id, rdf_type_uri) VALUES (2, ?)",
                RdfLexicon.NON_RDF_SOURCE.getURI());
        jdbcTemplate.update("INSERT INTO search_rdf_type (id, rdf_type_uri) VALUES (3, ?)",
                RdfLexicon.FEDORA_RESOURCE.getURI());
        jdbcTemplate.update("INSERT INTO search_rdf_type (id, rdf_type_uri) VALUES (4, ?)",
                RdfLexicon.RDF_SOURCE.getURI());

        // Insert test resources
        insertContainerResource("resource1");
        insertBinaryResource("resource2", "text/plain", 1024L);
        insertBinaryResource("resource3", "image/jpeg", 2048L);
        insertBinaryResource("resource4", "text/plain", 512L);
    }

    private void insertContainerResource(final String fedoraId) {
        final int id = nextId++;
        jdbcTemplate.update("INSERT INTO simple_search (id, fedora_id, created, modified) VALUES (?, ?, ?, ?)",
                id, "info:fedora/" + fedoraId, Instant.now(), Instant.now());

        // Add container RDF types
        jdbcTemplate.update("INSERT INTO search_resource_rdf_type (resource_id, rdf_type_id) VALUES (?, ?)",
                id, 1); // BASIC_CONTAINER
        jdbcTemplate.update("INSERT INTO search_resource_rdf_type (resource_id, rdf_type_id) VALUES (?, ?)",
                id, 3); // FEDORA_RESOURCE
        jdbcTemplate.update("INSERT INTO search_resource_rdf_type (resource_id, rdf_type_id) VALUES (?, ?)",
                id, 4); // RDF_SOURCE
    }

    private void insertBinaryResource(final String fedoraId, final String mimeType, final Long contentSize) {
        final int id = nextId++;
        jdbcTemplate.update("INSERT INTO simple_search (id, fedora_id, mime_type, content_size, created, modified)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                id, "info:fedora/" + fedoraId, mimeType, contentSize, Instant.now(), Instant.now());

        // Add binary RDF types
        jdbcTemplate.update("INSERT INTO search_resource_rdf_type (resource_id, rdf_type_id) VALUES (?, ?)",
                id, 2); // NON_RDF_SOURCE
        jdbcTemplate.update("INSERT INTO search_resource_rdf_type (resource_id, rdf_type_id) VALUES (?, ?)",
                id, 3); // FEDORA_RESOURCE
    }

    @Test
    public void testGetResourceCount() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        final RepositoryStatsResult result = repositoryStats.getResourceCount(params);

        assertNotNull(result);
        assertEquals(4, result.getResourceCount());
    }

    @Test
    public void testGetByMimeTypesNoFilters() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        final RepositoryStatsByMimeTypeResults results = repositoryStats.getByMimeTypes(params);

        assertNotNull(results);
        assertNotNull(results.getMimeTypes());
        assertEquals(2, results.getMimeTypes().size());

        // Verify text/plain results
        final MimeTypeStatsResult textPlainResult = findMimeTypeResult(results.getMimeTypes(), "text/plain");
        assertNotNull(textPlainResult);
        assertEquals(2, textPlainResult.getResourceCount());
        assertEquals(1536L, textPlainResult.getByteCount());

        // Verify image/jpeg results
        final MimeTypeStatsResult imageJpegResult = findMimeTypeResult(results.getMimeTypes(), "image/jpeg");
        assertNotNull(imageJpegResult);
        assertEquals(1, imageJpegResult.getResourceCount());
        assertEquals(2048L, imageJpegResult.getByteCount());
    }

    @Test
    public void testGetByMimeTypesWithFilter() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        params.setMimeTypes(Collections.singletonList("text/plain"));
        final RepositoryStatsByMimeTypeResults results = repositoryStats.getByMimeTypes(params);

        assertNotNull(results);
        assertNotNull(results.getMimeTypes());
        assertEquals(1, results.getMimeTypes().size());

        // Verify text/plain results
        final MimeTypeStatsResult textPlainResult = results.getMimeTypes().get(0);
        assertEquals("text/plain", textPlainResult.getMimeType());
        assertEquals(2, textPlainResult.getResourceCount());
        assertEquals(1536L, textPlainResult.getByteCount());
    }

    @Test
    public void testGetByMimeTypesWithMultipleFilters() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        params.setMimeTypes(Arrays.asList("text/plain", "image/jpeg"));
        final RepositoryStatsByMimeTypeResults results = repositoryStats.getByMimeTypes(params);

        assertNotNull(results);
        assertNotNull(results.getMimeTypes());
        assertEquals(2, results.getMimeTypes().size());
    }

    @Test
    public void testGetByMimeTypesWithNonExistentFilter() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        params.setMimeTypes(Collections.singletonList("application/json"));
        final RepositoryStatsByMimeTypeResults results = repositoryStats.getByMimeTypes(params);

        assertNotNull(results);
        // Should return empty list since no resources match this mime type
        assertTrue(results.getMimeTypes().isEmpty());
    }

    @Test
    public void testGetByRdfTypeNoFilters() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        final RepositoryStatsByRdfTypeResults results = repositoryStats.getByRdfType(params);

        assertNotNull(results);
        assertNotNull(results.getRdfTypes());
        assertEquals(4, results.getRdfTypes().size());

        // Verify BASIC_CONTAINER results
        final var basicContainerResult = findRdfTypeResult(results.getRdfTypes(),
                RdfLexicon.BASIC_CONTAINER.getURI());
        assertNotNull(basicContainerResult);
        assertEquals(1, basicContainerResult.getResourceCount());
        assertEquals(0L, basicContainerResult.getByteCount());

        // Verify NON_RDF_SOURCE results
        final var nonRdfSourceResult = findRdfTypeResult(results.getRdfTypes(),
                RdfLexicon.NON_RDF_SOURCE.getURI());
        assertNotNull(nonRdfSourceResult);
        assertEquals(3, nonRdfSourceResult.getResourceCount());
        assertEquals(3584L, nonRdfSourceResult.getByteCount());
    }

    @Test
    public void testGetByRdfTypeWithFilter() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        params.setRdfTypes(Collections.singletonList(RdfLexicon.BASIC_CONTAINER.getURI()));
        final RepositoryStatsByRdfTypeResults results = repositoryStats.getByRdfType(params);

        assertNotNull(results);
        assertNotNull(results.getRdfTypes());
        assertEquals(1, results.getRdfTypes().size());

        // Verify BASIC_CONTAINER results
        final var basicContainerResult = results.getRdfTypes().get(0);
        assertEquals(RdfLexicon.BASIC_CONTAINER.getURI(), basicContainerResult.getResourceType());
        assertEquals(1, basicContainerResult.getResourceCount());
        assertEquals(0L, basicContainerResult.getByteCount());
    }

    @Test
    public void testGetByRdfTypeWithMultipleFilters() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        params.setRdfTypes(Arrays.asList(RdfLexicon.BASIC_CONTAINER.getURI(), RdfLexicon.NON_RDF_SOURCE.getURI()));
        final RepositoryStatsByRdfTypeResults results = repositoryStats.getByRdfType(params);

        assertNotNull(results);
        assertNotNull(results.getRdfTypes());
        assertEquals(2, results.getRdfTypes().size());
    }

    @Test
    public void testGetByRdfTypeWithNonExistentFilter() {
        final RepositoryStatsParameters params = new RepositoryStatsParameters();
        params.setRdfTypes(Collections.singletonList("http://example.org/nonexistent"));
        final RepositoryStatsByRdfTypeResults results = repositoryStats.getByRdfType(params);

        assertNotNull(results);
        // Should return empty list since no resources match this RDF type
        assertTrue(results.getRdfTypes().isEmpty());
    }

    /**
     * Helper method to find a MimeTypeStatsResult by mime type
     */
    private MimeTypeStatsResult findMimeTypeResult(final List<MimeTypeStatsResult> results, final String mimeType) {
        return results.stream()
                .filter(result -> mimeType.equals(result.getMimeType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Helper method to find an RdfTypeStatsResult by RDF type URI
     */
    private RdfTypeStatsResult findRdfTypeResult(final List<RdfTypeStatsResult> results, final String rdfType) {
        return results.stream()
                .filter(result -> rdfType.equals(result.getResourceType()))
                .findFirst()
                .orElse(null);
    }
}