/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.api.repository;

import javax.jcr.Session;
import javax.ws.rs.Path;

import org.fcrepo.api.FedoraTransactions;
import org.fcrepo.session.InjectedSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This stub is a hack to mount the functionality of FedoraTransactions at the
 * root of this webapp. Without it, the globbing from FedoraNodes would own this
 * path instead.
 */
@Component
@Scope("prototype")
@Path("/fcr:tx")
public class FedoraRepositoryTransactions extends FedoraTransactions {

    @InjectedSession
    protected Session session;
}
