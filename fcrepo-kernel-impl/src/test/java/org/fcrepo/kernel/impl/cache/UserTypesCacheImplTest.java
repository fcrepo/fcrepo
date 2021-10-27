/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.cache;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFFormat.NTRIPLES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Before;
import org.junit.Test;

/**
 * @author pwinckles
 */
public class UserTypesCacheImplTest {

    private static final URI ANIMAL_TYPE = URI.create("http://example.org/#Animal");
    private static final URI CAT_TYPE = URI.create("http://example.org/#Cat");

    private UserTypesCacheImpl cache;

    private FedoraId fedoraId;

    private String sessionId;

    private String sessionId2;

    @Before
    public void setup() {
        final var props = new FedoraPropsConfig();
        props.setUserTypesCacheSize(1024);
        props.setUserTypesCacheTimeout(10);
        cache = new UserTypesCacheImpl(props);

        fedoraId = FedoraId.create(UUID.randomUUID().toString());
        sessionId = UUID.randomUUID().toString();
        sessionId2 = UUID.randomUUID().toString();
    }

    @Test
    public void loadTriplesWhenNotCached() {
        final var types = cache.getUserTypes(fedoraId, sessionId, rdfSupplier(ANIMAL_TYPE, CAT_TYPE));
        assertThat(types, containsInAnyOrder(ANIMAL_TYPE, CAT_TYPE));
    }

    @Test
    public void loadFromCacheWhenCached() {
        cache.getUserTypes(fedoraId, sessionId, rdfSupplier(ANIMAL_TYPE, CAT_TYPE));

        final var types = cache.getUserTypes(fedoraId, sessionId, noCallSupplier());
        assertThat(types, containsInAnyOrder(ANIMAL_TYPE, CAT_TYPE));
    }

    @Test
    public void manuallyCachedTypesAreOnlyAvailableInSession() {
        cache.cacheUserTypes(fedoraId, List.of(ANIMAL_TYPE), sessionId);

        var types = cache.getUserTypes(fedoraId, sessionId, noCallSupplier());
        assertThat(types, containsInAnyOrder(ANIMAL_TYPE));

        types = cache.getUserTypes(fedoraId, sessionId2, rdfSupplier(ANIMAL_TYPE, CAT_TYPE));
        assertThat(types, containsInAnyOrder(ANIMAL_TYPE, CAT_TYPE));
    }

    @Test
    public void mergeCacheMakesTypesGloballyAvailable() {
        cache.cacheUserTypes(fedoraId, List.of(ANIMAL_TYPE), sessionId);
        cache.mergeSessionCache(sessionId);

        final var types = cache.getUserTypes(fedoraId, sessionId2, noCallSupplier());
        assertThat(types, containsInAnyOrder(ANIMAL_TYPE));
    }

    @Test
    public void droppingCacheRemovesSessionCache() {
        cache.cacheUserTypes(fedoraId, List.of(ANIMAL_TYPE), sessionId);

        var types = cache.getUserTypes(fedoraId, sessionId, noCallSupplier());
        assertThat(types, containsInAnyOrder(ANIMAL_TYPE));

        cache.dropSessionCache(sessionId);

        types = cache.getUserTypes(fedoraId, sessionId, rdfSupplier(CAT_TYPE));
        assertThat(types, containsInAnyOrder(CAT_TYPE));
    }

    private Supplier<RdfStream> noCallSupplier() {
        return () -> {
            throw new RuntimeException("Should not be called");
        };
    }

    private Supplier<RdfStream> rdfSupplier(final URI... types) {
        return () -> createRdfStream(types);
    }

    private RdfStream createRdfStream(final URI... types) {
        final var builder = new StringBuilder();

        for (final var type : types) {
            builder.append("<> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <").append(type).append(">.\n");
        }

        final Model model = createDefaultModel();
        RDFDataMgr.read(model, IOUtils.toInputStream(builder.toString(), StandardCharsets.UTF_8), NTRIPLES.getLang());
        return DefaultRdfStream.fromModel(createURI(fedoraId.getFullId()), model);
    }

}
