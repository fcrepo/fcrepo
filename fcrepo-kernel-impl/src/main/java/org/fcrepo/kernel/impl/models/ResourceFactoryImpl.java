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

import org.fcrepo.kernel.api.FedoraTransaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.FedoraTimeMap;
import org.fcrepo.kernel.api.models.FedoraWebacAcl;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionFactory;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;


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
    public Container createContainer(final FedoraTransaction transaction, final String identifier) {
        // TODO: Change to ContainerImpl.class when implemented
        return (Container) createResource(Container.class);
    }

    @Override
    public FedoraBinary createBinary(final FedoraTransaction transaction, final String identifier) {
        // TODO: Change to FedoraBinaryImpl.class when implemented
        return (FedoraBinary) createResource(FedoraBinary.class);
    }

    @Override
    public NonRdfSourceDescription createBinaryDescription(final FedoraTransaction transaction,
            final String identifier) {
        // TODO: Change to NonRdfSourceDescrptionImpl.class when implemented
        return (NonRdfSourceDescription) createResource(NonRdfSourceDescription.class);
    }

    @Override
    public FedoraTimeMap createTimemap(final FedoraTransaction transaction, final String identifier) {
        // TODO: Change to FedoraTimeMapImpl.class when implemented
        return (FedoraTimeMap) createResource(FedoraTimeMap.class);
    }

    @Override
    public FedoraWebacAcl createAcl(final FedoraTransaction transaction, final String identifier) {
        // TODO: Change to ContainerImpl.class when implemented
        return (FedoraWebacAcl) createResource(FedoraWebacAcl.class);
    }

    @Override
    public FedoraResource getResource(final FedoraTransaction transaction, final String identifier)
            throws PathNotFoundException {
        try {
            final PersistentStorageSession psSession = getSession(transaction);
            return psSession.read(identifier);
        } catch (final PersistentItemNotFoundException e) {
            throw new PathNotFoundException(e);
        } catch (final PersistentStorageException e) {
            // This is a big error, wrap as RepositoryRuntime and send it through.
            throw new RepositoryRuntimeException(e);
        }

    }

    @Override
    public <T extends FedoraResource> T getResource(final FedoraTransaction transaction, final String identifier,
            final Class<T> clazz)
            throws PathNotFoundException {
        try {
            final PersistentStorageSession psSession = getSession(transaction);
            return clazz.cast(psSession.read(identifier));
        } catch (final PersistentItemNotFoundException e) {
            throw new PathNotFoundException(e);
        } catch (final PersistentStorageException e) {
            // This is a big error, wrap as RepositoryRuntime and send it through.
            throw new RepositoryRuntimeException(e);
        }

    }

    /**
     * This is probably a bad idea, but for stubbing lets instantiate whatever we need.
     *
     * @param createClass The class of the object to make.
     * @return FedoraResource sub-type object of class c
     */
    private FedoraResource createResource(final Class<? extends FedoraResource> createClass) {
        try {
            return createClass.newInstance();
        } catch (final InstantiationException e) {
            throw new RepositoryRuntimeException(e);
        } catch (final IllegalAccessException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Get a session for this interaction.
     *
     * @param transaction The supplied transaction id.
     * @return a storage session.
     */
    private PersistentStorageSession getSession(final FedoraTransaction transaction) {
        final PersistentStorageSession session;
        if (transaction == null) {
            session = factory.getReadOnlySession();
        } else {
            session = factory.getSession(transaction.getId());
        }
        return session;
    }
}
