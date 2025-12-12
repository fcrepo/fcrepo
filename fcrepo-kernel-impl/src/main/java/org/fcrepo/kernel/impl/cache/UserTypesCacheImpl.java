/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.cache;

import static java.util.stream.Collectors.toList;
import static org.apache.jena.vocabulary.RDF.type;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.apache.jena.graph.Triple;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Default UserTypesCache implementation
 *
 * @author pwinckles
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class UserTypesCacheImpl implements UserTypesCache {

    private final Cache<FedoraId, List<URI>> globalCache;
    private final Map<String, Cache<FedoraId, List<URI>>> sessionCaches;

    public UserTypesCacheImpl(final FedoraPropsConfig config) {
        this.globalCache = Caffeine.newBuilder()
                .maximumSize(config.getUserTypesCacheSize())
                .expireAfterAccess(config.getUserTypesCacheTimeout(), TimeUnit.MINUTES)
                .build();
        this.sessionCaches = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<URI> getUserTypes(final FedoraId resourceId,
                                  final String sessionId,
                                  final Supplier<RdfStream> rdfProvider) {
        if (isNotReadOnlySession(sessionId)) {
            final var sessionCache = getSessionCache(sessionId);

            return sessionCache.get(resourceId, k -> {
                return globalCache.get(resourceId, k2 -> {
                    return extractRdfTypes(rdfProvider.get());
                });
            });
        } else {
            return globalCache.get(resourceId, k -> {
                return extractRdfTypes(rdfProvider.get());
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cacheUserTypes(final FedoraId resourceId,
                               final RdfStream rdf,
                               final String sessionId) {
        if (isNotReadOnlySession(sessionId)) {
            getSessionCache(sessionId).put(resourceId, extractRdfTypes(rdf));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cacheUserTypes(final FedoraId resourceId,
                               final List<URI> userTypes,
                               final String sessionId) {
        if (isNotReadOnlySession(sessionId)) {
            getSessionCache(sessionId).put(resourceId, userTypes);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mergeSessionCache(final String sessionId) {
        if (isNotReadOnlySession(sessionId)) {
            final var sessionCache = getSessionCache(sessionId);
            globalCache.putAll(sessionCache.asMap());
            dropSessionCache(sessionId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dropSessionCache(final String sessionId) {
        if (isNotReadOnlySession(sessionId)) {
            sessionCaches.remove(sessionId);
        }
    }

    private Cache<FedoraId, List<URI>> getSessionCache(final String sessionId) {
        return sessionCaches.computeIfAbsent(sessionId, k -> {
            return Caffeine.newBuilder()
                    .maximumSize(1024)
                    .build();
        });
    }

    private List<URI> extractRdfTypes(final RdfStream rdf) {
        return rdf.filter(t -> t.predicateMatches(type.asNode()))
                .map(Triple::getObject)
                .map(t -> URI.create(t.toString()))
                .collect(toList());
    }

    private boolean isNotReadOnlySession(final String sessionId) {
        return !ReadOnlyTransaction.READ_ONLY_TX_ID.equals(sessionId);
    }
}
