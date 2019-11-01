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
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;

/**
 * @author cabeer
 * @since 10/10/14
 */
public interface BinaryService extends Service<Binary> {

    /**
     * Retrieves a Binary instance by transaction and path.
     *
     * @param transaction transaction
     * @param path path of binary datastream
     * @return retrieved Binary
     */
    Binary findOrCreateBinary(Transaction transaction, String path);

    /**
     * Retrieves a binary description instance by transaction and path.
     *
     * @param transaction transaction
     * @param path path of description
     * @return retrieved NonRdfSourceDescription
     */
    NonRdfSourceDescription findOrCreateDescription(Transaction transaction, String path);

}