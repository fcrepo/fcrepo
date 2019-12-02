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
package org.fcrepo.kernel.api.operations;

import java.time.Instant;

import org.fcrepo.kernel.api.RdfStream;

/**
 * Operation for interacting with an rdf source
 *
 * @author bbpennel
 */
public interface RdfSourceOperation extends ResourceOperation {

    /**
     * Get the incoming user space triples for the resource
     *
     * @return triples
     */
    RdfStream getTriples();

    /**
     * Get last modified by
     *
     * @return user that last modified the resource
     */
    String getLastModifiedBy();

    /**
     * Get created by
     *
     * @return user that created the resource
     */
    String getCreatedBy();

    /**
     * Get the timestamp the resource was last modified
     *
     * @return timestamp
     */
    Instant getLastModifiedDate();

    /**
     * Get the timestamp the resource was created
     *
     * @return timestamp
     */
    Instant getCreatedDate();
}
