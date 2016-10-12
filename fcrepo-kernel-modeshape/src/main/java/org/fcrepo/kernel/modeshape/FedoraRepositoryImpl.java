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
package org.fcrepo.kernel.modeshape;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * The basic abstraction for a Fedora repository
 * @author acoburn
 */
public class FedoraRepositoryImpl implements FedoraRepository {

    private final Repository repository;

    /**
     * Create a FedoraRepositoryImpl with a JCR-based Repository
     *
     * @param repository the JCR repository
     */
    public FedoraRepositoryImpl(final Repository repository) {
        this.repository = repository;
    }

    @Override
    public FedoraSession login() {
        try {
            return new FedoraSessionImpl(repository.login());
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }

    @Override
    public FedoraSession login(final Object credentials) {
        if (credentials instanceof Credentials) {
            try {
                return new FedoraSessionImpl(repository.login((Credentials) credentials));
            } catch (final RepositoryException ex) {
                throw new RepositoryRuntimeException(ex);
            }
        }
        throw new ClassCastException("login credentials are not an instance of " +
                Credentials.class.getCanonicalName());
    }

    /**
     * Retrieve the internal JCR Repository object
     *
     * @return the JCR Repository
     */
    public Repository getJcrRepository() {
        return repository;
    }

    /**
     * Retrieve the internal JCR Repository from a FedoraRepository object
     *
     * @param repository the FedoraRepository
     * @return the JCR Repository
     */
    public static Repository getJcrRepository(final FedoraRepository repository) {
        if (repository instanceof FedoraRepositoryImpl) {
            return ((FedoraRepositoryImpl)repository).getJcrRepository();
        }
        throw new ClassCastException("FedoraRepository is not a " + FedoraRepositoryImpl.class.getCanonicalName());
    }
}
