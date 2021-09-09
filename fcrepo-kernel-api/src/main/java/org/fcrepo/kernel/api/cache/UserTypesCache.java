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

package org.fcrepo.kernel.api.cache;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;

// TODO docs
public interface UserTypesCache {

    List<URI> getUserTypes(final FedoraId resourceId,
                           final String sessionId,
                           final Supplier<RdfStream> rdfProvider);

    void cacheUserTypes(final FedoraId resourceId,
                        final RdfStream rdf,
                        final String sessionId);

    void cacheUserTypes(final FedoraId resourceId,
                        final List<URI> userTypes,
                        final String sessionId);

    void mergeSessionCache(final String sessionId);

    void dropSessionCache(final String sessionId);

}
