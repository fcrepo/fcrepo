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

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Service for creating versions of resources
 *
 * @author bbpennel
 * @author whikloj
 * @since Feb 19, 2014
 */
public interface VersionService {

    /**
     * To format a datetime for use as a Memento path.
     */
    DateTimeFormatter MEMENTO_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(UTC);

    /**
     * To format a datetime as RFC-1123 with correct timezone.
     */
    DateTimeFormatter MEMENTO_RFC_1123_FORMATTER = RFC_1123_DATE_TIME.withZone(UTC);

    /**
     * Explicitly creates a version for the resource at the path provided.
     *
     * @param transaction the transaction in which the resource resides
     * @param fedoraId the internal resource id
     * @param userPrincipal the user principal
     */
    void createVersion(Transaction transaction, FedoraId fedoraId, String userPrincipal);

}
