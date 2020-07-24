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
package org.fcrepo.persistence.ocfl.impl;

import javax.annotation.Nonnull;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An simple in-memory implementation of the {@link FedoraToOcflObjectIndex} used for testing
 *
 * @author pwinckles
 */
public class TestOcflObjectIndex implements FedoraToOcflObjectIndex {

    private static Logger LOGGER = LoggerFactory.getLogger(TestOcflObjectIndex.class);

    private Map<FedoraId, FedoraOcflMapping> fedoraOcflMappingMap = Collections.synchronizedMap(new HashMap<>());

    @Override
    public FedoraOcflMapping getMapping(final String transactionId, final FedoraId fedoraResourceIdentifier)
            throws FedoraOcflMappingNotFoundException {

        LOGGER.debug("getting {}", fedoraResourceIdentifier);
        final FedoraOcflMapping m = fedoraOcflMappingMap.get(fedoraResourceIdentifier);
        if (m == null) {
            throw new FedoraOcflMappingNotFoundException(fedoraResourceIdentifier.getFullId());
        }

        return m;
    }

    @Override
    public FedoraOcflMapping addMapping(@Nonnull final String transactionId,
                                        final FedoraId fedoraResourceIdentifier,
                                        final FedoraId fedoraRootObjectResourceId,
                                        final String ocflObjectId) {
        FedoraOcflMapping mapping = fedoraOcflMappingMap.get(fedoraRootObjectResourceId);

        if (mapping == null) {
            mapping = new FedoraOcflMapping(fedoraRootObjectResourceId, ocflObjectId);
            fedoraOcflMappingMap.put(fedoraRootObjectResourceId, mapping);
        }

        if (!fedoraResourceIdentifier.equals(fedoraRootObjectResourceId)) {
            fedoraOcflMappingMap.put(fedoraResourceIdentifier, mapping);
        }

        LOGGER.debug("added mapping {} for {}", mapping, fedoraResourceIdentifier);
        return mapping;
    }

    @Override
    public void removeMapping(@Nonnull final String transactionId, final FedoraId fedoraResourceIdentifier) {
            fedoraOcflMappingMap.remove(fedoraResourceIdentifier);
    }

    @Override
    public void reset() {
        fedoraOcflMappingMap.clear();
    }

    @Override
    public void commit(@Nonnull final String sessionId) {

    }

    @Override
    public void rollback(@Nonnull final String sessionId) {

    }

}
