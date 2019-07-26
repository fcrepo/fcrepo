package org.fcrepo.kernel.impl;

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

import static org.fcrepo.persistence.api.Options.Option.ARCHIVAL_GROUP;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.persistence.api.Options;
import org.fcrepo.persistence.api.PersistentResource;
import org.fcrepo.persistence.api.PersistentStorage;
import org.fcrepo.persistence.api.PersistentStorageSession;

/**
 * @author dbernstein
 */
public class ContainerServiceImpl implements ContainerService {

    private PersistentStorage persistentStorage;

    @Override
    public Container findOrCreate(final FedoraSession session, final String path, final String interactionModel) {

        //TODO validate interaction model
        //if archival group, ensure parent is not archival group

        final Container container = find(session, path);
        if (container == null) {
            return create(session, path, interactionModel);
        } else {
            return container;
        }
    }

    private Container create(final FedoraSession session, final String path, final String interactionModel) {
        final String psSessionId = resolvePeristentSessionId(session);
        final PersistentStorageSession storageSession = persistentStorage.getSession(psSessionId);
        final Options options = new Options();
        if(isArchivalGroup(interactionModel)){
            options.add(ARCHIVAL_GROUP);
        }

        final PersistentResource persistentObject = persistentStorage.create(storageSession, path, options);
        return toContainer(session, storageSession, persistentObject);
    }

    private boolean isArchivalGroup(final String interactionModel) {
        //TODO implement this method
        return true;
    }

    private Container toContainer(final FedoraSession session, final PersistentStorageSession storageSession,
                                  final PersistentResource persistentObject) {
        return null;
    }

    private String resolvePeristentSessionId(final FedoraSession session) {
        //TODO generate a unique session id based on the fedora session info.
        return null;
    }

    @Override
    public boolean exists(final FedoraSession session, final String path) {
        return find(session, path) != null;
    }

    @Override
    public Container find(final FedoraSession session, final String path) {
        final String psSessionId = resolvePeristentSessionId(session);
        final PersistentStorageSession storageSession = persistentStorage.getSession(psSessionId);
        final PersistentResource persistentObject = persistentStorage.get(storageSession, path);
        if(persistentObject != null){
            return toContainer(session, storageSession, persistentObject);
        } else {
            return null;
        }
    }

    @Override
    public Container findOrCreate(final FedoraSession session, final String path) {
        return findOrCreate(session, path, null);
    }
}
