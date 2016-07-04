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
package org.fcrepo.http.commons.api;

import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.models.FedoraResource;

import com.google.common.collect.Multimap;

/**
 * Helper interface to inject Http Headers from external modules.
 *
 * @author whikloj
 * @since 2015-11-01
 */
public interface UriAwareHttpHeaderFactory {

    /**
     * Given a resource and session, update the JAX-RS response with any additional headers.
     *
     * @param uriInfo contextual information for building URLs
     * @param resource the resource from the request
     * @return Multimap of HTTP Header field names and field values
     */
    Multimap<String, String> createHttpHeadersForResource(UriInfo uriInfo, FedoraResource resource);

}
