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

public final class FedoraUtils {

    /**
     * Add a subpath to an existing identifier.
     *
     * @param oldId the old identifier
     * @param newIdPart the new identifier part
     * @return A combination of old and new.
     */
    public static String addToIdentifier(final String oldId, final String newIdPart) {
        return oldId + (oldId.endsWith("/") ? "" : "/") + newIdPart;
    }

    /**
     * Private Constructor.
     */
    private FedoraUtils() {
        // This constructor left intentionally blank.
    }
}
