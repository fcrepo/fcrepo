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

import java.lang.reflect.Constructor;
import java.util.Collection;
import javax.inject.Inject;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionFactory;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;


/**
 * Implementation of ResourceFactory interface.
 *
 * @author whikloj
 * @since 2019-09-23
 */
public class ResourceFactoryImpl implements ResourceFactory {

    /**
     * Singleton factory;
     */
    private static ResourceFactory instance = null;

    @Inject
    private static PersistentStorageSessionFactory factory;

    /**
     * Private constructor
     */
    private ResourceFactoryImpl() {
    }

    /**
     * Get instance of ResourceFactory.
     *
     * @return the singleton ResourceFactory.
     */
    public static ResourceFactory getInstance() {
        if (instance == null) {
            instance = new ResourceFactoryImpl();
        }
        return instance;
    }

    @Override
    public Container createContainer(final Transaction transaction, final String identifier) {
        // TODO: Change to ContainerImpl.class when implemented
        return (Container) createResource(transaction, identifier, Container.class);
    }

    @Override
    public Binary createBinary(final Transaction transaction, final String identifier) {
        // TODO: Change to FedoraBinaryImpl.class when implemented
        return (Binary) createResource(transaction, identifier, Binary.class);
    }

    @Override
    public NonRdfSourceDescription createBinaryDescription(final Transaction transaction,
            final String identifier) {
        // TODO: Change to NonRdfSourceDescrptionImpl.class when implemented
        return (NonRdfSourceDescription) createResource(transaction, identifier, NonRdfSourceDescription.class);
    }

    @Override
    public TimeMap createTimemap(final Transaction transaction, final String identifier) {
        // TODO: Change to FedoraTimeMapImpl.class when implemented
        return (TimeMap) createResource(transaction, identifier, TimeMap.class);
    }

    @Override
    public WebacAcl createAcl(final Transaction transaction, final String identifier) {
        // TODO: Change to ContainerImpl.class when implemented
        return (WebacAcl) createResource(transaction, identifier, WebacAcl.class);
    }

    @Override
    public FedoraResource getResource(final Transaction transaction, final String identifier)
            throws PathNotFoundException {
        try {
            final PersistentStorageSession psSession = getSession(transaction);
            final ResourceHeaders headers = psSession.getHeaders(identifier);
            final Class<? extends FedoraResource> clazz = getClassForTypes(headers);

            final FedoraResource resource = createResource(transaction, identifier, clazz);
            resource.setHeaders(headers);

            return resource;
        } catch (final PersistentItemNotFoundException e) {
            throw new PathNotFoundException(e);
        }
    }

    @Override
    public <T extends FedoraResource> T getResource(final Transaction transaction, final String identifier,
            final Class<T> clazz)
            throws PathNotFoundException {
        return clazz.cast(getResource(transaction, identifier));
    }

    /**
     * Returns the appropriate FedoraResource class for an object based on the provided headers
     *
     * @param headers headers for the resource being constructed
     * @return FedoraResource class
     */
    private Class<? extends FedoraResource> getClassForTypes(final ResourceHeaders headers) {
        final Collection<String> types = headers.getTypes();
        if (types.contains(BASIC_CONTAINER.toString()) || types.contains(INDIRECT_CONTAINER.toString()) || types
                .contains(DIRECT_CONTAINER.toString())) {
            return Container.class;
        }
        if (types.contains(NON_RDF_SOURCE.toString())) {
            return Binary.class;
        }
        // TODO add the rest of the types
        throw new ResourceTypeException("Could not identify the resource type from values " + types.toString());
    }

    /**
     * Instantiates a new FedoraResource object of the given class.
     *
     * @param transaction the transaction
     * @param identifier identifier for the new instance
     * @param createClass class of the new resource
     * @return new FedoraResource instance
     */
    private FedoraResource createResource(final Transaction transaction, final String identifier,
            final Class<? extends FedoraResource> createClass) {
        try {
            // Retrieve standard constructor
            final Constructor<? extends FedoraResource> constructor = createClass.getConstructor(
                    String.class, Transaction.class, PersistentStorageSessionFactory.class);

            return constructor.newInstance(identifier, transaction, factory);
        } catch (SecurityException | ReflectiveOperationException e) {
            throw new RepositoryRuntimeException("Unable to construct object", e);
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
            session = factory.getReadOnlySession();
        } else {
            session = factory.getSession(transaction.getId());
        }
        return session;
    }
}
