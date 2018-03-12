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
package org.fcrepo.kernel.api.services;

import java.time.Instant;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * @author bbpennel
 * @author whikloj
 * @since Feb 19, 2014
 */
public interface VersionService {

    /**
     * Explicitly creates a version for the resource at the path provided for now.
     *
     * @param session the session in which the resource resides
     * @param resource the resource to version
     * @return the version
     */
    FedoraResource createVersion(FedoraSession session, FedoraResource resource);

    /**
     * Explicitly creates a version for the resource at the path provided for the date/time provided.
     *
     * @param session the session in which the resource resides
     * @param resource the resource to version
     * @param dateTime the date/time of the version
     * @param fromExisting if true, version is created from existing resource. If false, version is created as a new
     *        resource.
     * @return the version
     */
    FedoraResource createVersion(FedoraSession session, FedoraResource resource, Instant dateTime,
            boolean fromExisting);

}
