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
package org.fcrepo.kernel.api.auth;

import java.util.List;

import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * Class to hold the authorizations from an ACL and the resource that has the ACL.
 *
 * @author whikloj
 */
public interface ACLHandle {

    /**
     * Get the resource that contains the ACL.
     *
     * @return the fedora resource
     */
    FedoraResource getResource();

    /**
     * Get the list of authorizations from the ACL.
     *
     * @return the authorizations
     */
    List<WebACAuthorization> getAuthorizations();
}
