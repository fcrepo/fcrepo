/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_WEBAC_ACL_URI;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

/**
 * Implementation of ResourceFactory interface.
 *
 * @author whikloj
 * @since 2019-09-23
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ResourceFactoryImpl implements ResourceFactory {

    private static final Logger LOGGER = getLogger(ResourceFactoryImpl.class);

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    @Inject
    private UserTypesCache userTypesCache;

    @Override
    public FedoraResource getResource(final Transaction transaction, final FedoraId fedoraID)
            throws PathNotFoundException {
        return instantiateResource(transaction, fedoraID);
    }

    @Override
    public <T extends FedoraResource> T getResource(final Transaction transaction, final FedoraId identifier,
                                                    final Class<T> clazz) throws PathNotFoundException {
        return clazz.cast(getResource(transaction, identifier));
    }

    @Override
    public FedoraResource getResource(final Transaction transaction,
                                      final ResourceHeaders headers)
            throws PathNotFoundException {
        return instantiateResource(transaction, headers.getId(), headers);
    }


    @Override
    public FedoraResource getContainer(final Transaction transaction, final FedoraId resourceId) {
        final String containerId = containmentIndex.getContainedBy(transaction, resourceId);
        if (containerId == null) {
            return null;
        }
        try {
            return getResource(transaction, FedoraId.create(containerId));
        } catch (final PathNotFoundException exc) {
            return null;
        }
    }

    /**
     * Returns the appropriate FedoraResource class for an object based on the provided headers
     *
     * @param headers headers for the resource being constructed
     * @return FedoraResource class
     */
    private Class<? extends FedoraResourceImpl> getClassForTypes(final ResourceHeaders headers) {
        final var ixModel = headers.getInteractionModel();
        if (BASIC_CONTAINER.getURI().equals(ixModel) || INDIRECT_CONTAINER.getURI().equals(ixModel)
                || DIRECT_CONTAINER.getURI().equals(ixModel)) {
            return ContainerImpl.class;
        }
        if (NON_RDF_SOURCE.getURI().equals(ixModel)) {
            return BinaryImpl.class;
        }
        if (FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI.equals(ixModel)) {
            return NonRdfSourceDescriptionImpl.class;
        }
        if (FEDORA_WEBAC_ACL_URI.equals(ixModel)) {
            return WebacAclImpl.class;
        }
        // TODO add the rest of the types
        throw new ResourceTypeException("Could not identify the resource type for interaction model " + ixModel);
    }

    private FedoraResource instantiateResource(final Transaction transaction,
                                               final FedoraId identifier)
            throws PathNotFoundException {
        try {
            // For descriptions and ACLs we need the actual endpoint.
            final var psSession = getSession(transaction);
            final Instant versionDateTime = identifier.isMemento() ? identifier.getMementoInstant() : null;

            final ResourceHeaders headers = psSession.getHeaders(identifier, versionDateTime);
            return instantiateResource(transaction, identifier, headers);
        } catch (final PersistentItemNotFoundException e) {
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Instantiates a new FedoraResource object of the given class.
     *
     * @param transaction the transaction id
     * @param identifier    identifier for the new instance
     * @return new FedoraResource instance
     * @throws PathNotFoundException
     */
    private FedoraResource instantiateResource(final Transaction transaction,
                                               final FedoraId identifier,
                                               final ResourceHeaders headers)
            throws PathNotFoundException {
        try {
            final Instant versionDateTime = identifier.isMemento() ? identifier.getMementoInstant() : null;

            // Determine the appropriate class from headers
            final var createClass = getClassForTypes(headers);

            // Retrieve standard constructor
            final var constructor = createClass.getConstructor(
                    FedoraId.class,
                    Transaction.class,
                    PersistentStorageSessionManager.class,
                    ResourceFactory.class,
                    UserTypesCache.class);

            // If identifier is to a TimeMap we need to avoid creating a original resource with a Timemap FedoraId
            final var instantiationId = identifier.isTimemap() ?
                    FedoraId.create(identifier.getResourceId()) : identifier;

            final var rescImpl = constructor.newInstance(instantiationId, transaction,
                    persistentStorageSessionManager, this, userTypesCache);
            populateResourceHeaders(rescImpl, headers, versionDateTime);

            if (headers.isDeleted()) {
                final var rootId = FedoraId.create(identifier.getBaseId());
                final var tombstone = new TombstoneImpl(rootId, transaction, persistentStorageSessionManager,
                        this, rescImpl);
                tombstone.setLastModifiedDate(headers.getLastModifiedDate());
                return tombstone;
            } else if (identifier.isTimemap()) {
                // If identifier is a TimeMap, now we can return the virtual resource.
                return rescImpl.getTimeMap();
            }
            return rescImpl;
        } catch (final SecurityException | ReflectiveOperationException e) {
            throw new RepositoryRuntimeException("Unable to construct object", e);
        } catch (final PersistentItemNotFoundException e) {
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException(e.getMessage(), e);
        }
    }

    private void populateResourceHeaders(final FedoraResourceImpl resc,
                                         final ResourceHeaders headers, final Instant version) {
        resc.setCreatedBy(headers.getCreatedBy());
        resc.setCreatedDate(headers.getCreatedDate());
        resc.setLastModifiedBy(headers.getLastModifiedBy());
        resc.setLastModifiedDate(headers.getLastModifiedDate());
        resc.setParentId(headers.getParent());
        resc.setArchivalGroupId(headers.getArchivalGroupId());
        resc.setEtag(headers.getStateToken());
        resc.setStateToken(headers.getStateToken());
        resc.setIsArchivalGroup(headers.isArchivalGroup());
        resc.setInteractionModel(headers.getInteractionModel());
        resc.setStorageRelativePath(headers.getStorageRelativePath());

        // If there's a version, then it's a memento
        if (version != null) {
            resc.setIsMemento(true);
            resc.setMementoDatetime(version);
        }

        if (resc instanceof Binary) {
            final var binary = (BinaryImpl) resc;
            binary.setContentSize(headers.getContentSize());
            binary.setExternalHandling(headers.getExternalHandling());
            binary.setExternalUrl(headers.getExternalUrl());
            binary.setDigests(headers.getDigests());
            binary.setFilename(headers.getFilename());
            binary.setMimeType(headers.getMimeType());
        }
    }

    /**
     * Get a session for this interaction.
     *
     * @param transaction The supplied transaction.
     * @return a storage session.
     */
    private PersistentStorageSession getSession(final Transaction transaction) {
        final PersistentStorageSession session;
        if (transaction.isReadOnly() || !transaction.isOpen()) {
            session = persistentStorageSessionManager.getReadOnlySession();
        } else {
            session = persistentStorageSessionManager.getSession(transaction);
        }
        return session;
    }

    @Override
    public Stream<FedoraResource> getChildren(final Transaction transaction, final FedoraId resourceId) {
        return containmentIndex.getContains(transaction, resourceId)
            .map(childId -> {
                try {
                    return getResource(transaction, FedoraId.create(childId));
                } catch (final PathNotFoundException e) {
                    throw new PathNotFoundRuntimeException(e.getMessage(), e);
                }
            });
    }
}
