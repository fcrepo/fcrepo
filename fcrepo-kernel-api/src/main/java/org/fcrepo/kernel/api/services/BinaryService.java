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

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;

/**
 * @author cabeer
 * @since 10/10/14
 */
public interface BinaryService extends Service<FedoraBinary> {

    /**
     * Retrieves a FedoraBinary instance by session and path.
     *
     * @param session session
     * @param path path of binary datastream
     * @return retrieved FedoraBinary
     */
    FedoraBinary findOrCreateBinary(FedoraSession session, String path);

    /**
     * Retrieves a binary description instance by session and path.
     *
     * @param session session
     * @param path path of description
     * @return retrieved NonRdfSourceDescription
     */
    NonRdfSourceDescription findOrCreateDescription(FedoraSession session, String path);

}
