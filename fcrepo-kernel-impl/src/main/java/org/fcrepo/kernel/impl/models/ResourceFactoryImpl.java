/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.models;

import javax.inject.Inject;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;

import java.net.URI;
import java.util.stream.Collectors;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.ResourceHeaders;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;


/**
 * Implementation of ResourceFactory interface.
 *
 * @author whikloj
 * @since 2019-09-23
 */
public class ResourceFactoryImpl implements ResourceFactory {

    @Inject
    private static PersistentStorageSessionManager persistentStorageSessionManager;

    @Override
    public FedoraResource getResource(final String identifier)
            throws PathNotFoundException {
        return getResource(null, identifier);
    }

    @Override
    public FedoraResource getResource(final Transaction transaction, final String identifier)
            throws PathNotFoundException {
        return instantiateResource(transaction, identifier);
    }

    @Override
    public <T extends FedoraResource> T getResource(final String identifier, final Class<T> clazz)
            throws PathNotFoundException {
        return clazz.cast(getResource(null, identifier));
    }

    @Override
    public <T extends FedoraResource> T getResource(final Transaction transaction, final String identifier,
            final Class<T> clazz) throws PathNotFoundException {
        return clazz.cast(getResource(transaction, identifier));
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
        // TODO add the rest of the types
        throw new ResourceTypeException("Could not identify the resource type for interaction model " + ixModel);
    }

    /**
     * Instantiates a new FedoraResource object of the given class.
     *
     * @param transaction the transaction
     * @param identifier identifier for the new instance
     * @return new FedoraResource instance
     * @throws PathNotFoundException
     */
    private FedoraResource instantiateResource(final Transaction transaction, final String identifier)
            throws PathNotFoundException {
        try {
            final var psSession = getSession(transaction);
            final var headers = psSession.getHeaders(identifier, null);

            // Determine the appropriate class from headers
            final var createClass = getClassForTypes(headers);

            // Retrieve standard constructor
            final var constructor = createClass.getConstructor(
                    String.class,
                    Transaction.class,
                    PersistentStorageSessionManager.class,
                    ResourceFactory.class);

            final var rescImpl = constructor.newInstance(identifier, transaction,
                    persistentStorageSessionManager, this);
            populateResourceHeaders(rescImpl, headers);

            return rescImpl;
        } catch (SecurityException | ReflectiveOperationException e) {
            throw new RepositoryRuntimeException("Unable to construct object", e);
        } catch (final PersistentItemNotFoundException e) {
            throw new PathNotFoundException(e);
        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private void populateResourceHeaders(final FedoraResourceImpl resc, final ResourceHeaders headers) {
        resc.setCreatedBy(headers.getCreatedBy());
        resc.setCreatedDate(headers.getCreatedDate());
        resc.setLastModifiedBy(headers.getLastModifiedBy());
        resc.setLastModifiedDate(headers.getLastModifiedDate());
        resc.setParentId(headers.getParent());
        resc.setEtag(headers.getStateToken());
        resc.setStateToken(headers.getStateToken());
        if (headers.getTypes() != null) {
            resc.setTypes(headers.getTypes().stream().map(URI::create).collect(Collectors.toList()));
        }

        if (resc instanceof Binary) {
            // set binary headers
        }
    }

    /**
     * Get a session for this interaction.
     *
     * @param transaction The supplied transaction id.
     * @return a storage session.
     */
    private PersistentStorageSession getSession(final Transaction transaction) {
        final PersistentStorageSession session;
        if (transaction == null) {
            session = persistentStorageSessionManager.getReadOnlySession();
        } else {
            session = persistentStorageSessionManager.getSession(transaction.getId());
        }
        return session;
    }
}
