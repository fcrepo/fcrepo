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
package org.fcrepo.persistence.ocfl.api;

import java.nio.file.Path;

/**
 * A factory interface for creating {@link org.fcrepo.persistence.ocfl.api.OCFLObjectSession}.
 * @author dbernstein
 * @since 6.0.0
 */
public interface OCFLObjectSessionFactory {

    /**
     * Create new session.
     * @param ocflId The OCFL Object identifier
     * @param sessionStagingDir path to the staging directory for the storage session
     * @return The newly created session.
     */
    OCFLObjectSession create(final String ocflId, final Path sessionStagingDir);

}
