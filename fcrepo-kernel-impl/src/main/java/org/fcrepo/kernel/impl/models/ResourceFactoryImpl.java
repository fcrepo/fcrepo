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

import org.fcrepo.kernel.api.exception.InteractionModelViolationException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.FedoraTimeMap;
import org.fcrepo.kernel.api.models.FedoraWebacAcl;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionFactory;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;


/**
 * Implementation of ResourceFactory interface.
 *
 * @author whikloj
 * @since 2019-09-23
 */
public class ResourceFactoryImpl implements ResourceFactory {

    private final PersistentStorageSession psSession;

    /**
     * Special constructor for testing.
     *
     * @param psFactory Persistent storage session factory.
     */
    protected ResourceFactoryImpl(final PersistentStorageSessionFactory psFactory) {
        this.psSession = psFactory.getReadOnlySession();
    }

    @Override
    public Container findOrInitContainer(final String identifier, final String interactionModel) {
        final FedoraResource container = findInPersistence(identifier);
        if (!container.getTypes().stream().anyMatch(t -> interactionModel.equalsIgnoreCase(t.toString()))) {
            throw new InteractionModelViolationException("Actual interaction models did not match requested.");
        }
        return (Container) container;
    }

    @Override
    public FedoraBinary findOrInitBinary(final String identifier) {
        return (FedoraBinary) findInPersistence(identifier);
    }

    @Override
    public NonRdfSourceDescription findOrInitBinaryDescription(final String identifier) {
        return (NonRdfSourceDescription) findInPersistence(identifier);
    }

    @Override
    public FedoraTimeMap findOrInitTimemap(final String identifier) {
        return (FedoraTimeMap) findInPersistence(identifier);
    }

    @Override
    public FedoraWebacAcl findOrInitAcl(final String identifier) {
        return (FedoraWebacAcl) findInPersistence(identifier);
    }

    private FedoraResource findInPersistence(final String identifier) {
        try {
            return psSession.read(identifier);
        } catch (final PersistentStorageException e) {
            // Not found or error so create a new resource.
            // return new FedoraResource();
            return null;
        }
    }
}
