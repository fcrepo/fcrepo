/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api.repository;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.ws.rs.Path;

import org.fcrepo.http.api.FedoraImport;
import org.springframework.context.annotation.Scope;

/**
 * This stub is a hack to mount the functionality of FedoraImport at the root of
 * this webapp. Without it, the globbing from FedoraNodes would own this path
 * instead.
 *
 * @author cbeer
 *
 * @deprecated This endpoint will be removed and the feature will be implemented
 * via some Camel machinery.
 * See <a href="https://jira.duraspace.org/browse/FCREPO-1519">FCREPO-1519</a>.
 */
@Scope("prototype")
@Path("/fcr:import")
@Deprecated
public class FedoraRepositoryImport extends FedoraImport {

    @Inject
    protected Session session;

}
