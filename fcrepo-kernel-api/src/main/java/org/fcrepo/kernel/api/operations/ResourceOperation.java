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

/**
 * Operation for manipulating a resource
 *
 * @author bbpennel
 */
public interface ResourceOperation {

    /**
     * Id of the resource
     *
     * @return
     */
    String getResourceId();

    /**
     * Interaction model for the resource.
     *
     * @return
     */
    String getInteractionModel();

    /**
     * Get the created date for the resource
     *
     * @return created date
     */
    Instant getCreatedDate();

    /**
     * Get the last modified date for the resource
     *
     * @return last modified date
     */
    Instant getLastModifiedDate();

    /**
     * Get the created by user for the resource
     *
     * @return
     */
    String getCreatedBy();

    /**
     * Get the last modified by user for this resource
     *
     * @return
     */
    String getLastModifiedBy();
}
