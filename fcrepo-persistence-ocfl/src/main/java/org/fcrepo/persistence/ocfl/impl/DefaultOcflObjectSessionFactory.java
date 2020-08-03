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

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.ocfl.api.OcflObjectSession;
import org.fcrepo.persistence.ocfl.api.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;

import static org.fcrepo.persistence.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.api.CommitOption.UNVERSIONED;

/**
 * A default implemenntation of the {@link OcflObjectSessionFactory} interface.
 *
 * @author dbernstein
 * @since 6.0.0
 */
@Component
public class DefaultOcflObjectSessionFactory implements OcflObjectSessionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOcflObjectSessionFactory.class);

    /**
     * Controls whether changes are committed to new OCFL versions or to a mutable HEAD
     */
    @Value("${fcrepo.autoversioning.enabled:true}")
    private boolean autoVersioningEnabled;

    @Inject
    private MutableOcflRepository ocflRepository;

    @Override
    public OcflObjectSession create(final String ocflId,
                                    final Path sessionStagingDir) {
        return new DefaultOcflObjectSession(ocflId, objectStagingDir(sessionStagingDir, ocflId),
                this.ocflRepository, defaultCommitOption());
    }

    private CommitOption defaultCommitOption() {
        if (autoVersioningEnabled) {
            return NEW_VERSION;
        }
        return UNVERSIONED;
    }

    /**
     * Enable or disable auto versioning on future sessions the factory creates
     *
     * @param autoVersioningEnabled true if auto versioning is enabled
     */
    public void setAutoVersioningEnabled(final boolean autoVersioningEnabled) {
        this.autoVersioningEnabled = autoVersioningEnabled;
    }

    private Path objectStagingDir(final Path sessionStaging, final String objectIdentifier) {
        final var digest = DigestUtils.sha256Hex(objectIdentifier);
        return sessionStaging.resolve(digest);
    }

}
