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
package org.fcrepo.kernel.api.utils;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.services.VersionService;

/**
 * Converts an internal Fedora Resource ID into and internal ID that includes suffixes, such as "fcr:versions".
 */
public final class FedoraResourceIdConverter {

    private FedoraResourceIdConverter() {

    }

    /**
     * Converts an internal Fedora Resource ID into an internal ID that includes suffixes, such as "fcr:versions".
     * If no suffixes are needed, the internal ID is returned unchanged.
     *
     * @param resource the Fedora Resource
     * @return internal id with suffixes
     */
    public static String resolveFedoraId(final FedoraResource resource) {
        if (resource instanceof TimeMap) {
            return resource.getId() + "/" + FedoraTypes.FCR_VERSIONS;
        } else if (resource.isMemento()) {
            return resource.getId() + "/" +
                    FedoraTypes.FCR_VERSIONS + "/" +
                    VersionService.MEMENTO_LABEL_FORMATTER.format(resource.getMementoDatetime());
        }
        return resource.getId();
    }

}
