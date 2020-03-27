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
package org.fcrepo.kernel.impl.services.functions;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;

/**
 * This class provides some utilities to help with working with Fedora Ids
 * @author whikloj
 * @since 11/2019
 */

public final class FedoraIdUtils {

    /**
     * Ensure the ID has the info:fedora/ prefix.
     * @param id the identifier, if null assume repository root (info:fedora/)
     * @return the identifier with the info:fedora/ prefix.
     */
    public static String ensurePrefix(final String id) {
        if (id == null) {
            return FEDORA_ID_PREFIX;
        }
        return id.startsWith(FEDORA_ID_PREFIX) ? id : FEDORA_ID_PREFIX + id;
    }

    /**
     * Private Constructor.
     */
    private FedoraIdUtils() {
        // This constructor left intentionally blank.
    }
}
