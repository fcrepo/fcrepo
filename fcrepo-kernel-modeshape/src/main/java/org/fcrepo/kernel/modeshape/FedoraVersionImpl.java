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

import java.time.Instant;
import org.fcrepo.kernel.api.FedoraVersion;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author acoburn
 */
public class FedoraVersionImpl implements FedoraVersion {

    private static final Logger LOGGER = getLogger(FedoraVersionImpl.class);


    private final String identifier;
    private final Instant created;

    /**
     * Create an object that contains information about a particular FedoraResource version
     * @param identifier the identifier for this FedoraVersion
     * @param created the instant this version was created
     */
    public FedoraVersionImpl(final String identifier, final Instant created) {
        this.identifier = identifier;
        this.created = created;
    }

    @Override
    public String getIdentifier() {
        LOGGER.warn("Review if class can be removed after implementing Memento!");

        return identifier;
    }

    @Override
    public Instant getCreated() {
        LOGGER.warn("Review if class can be removed after implementing Memento!");
        return created;
    }
}
