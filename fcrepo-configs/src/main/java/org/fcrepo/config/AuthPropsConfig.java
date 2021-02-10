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

package org.fcrepo.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Auth related configuration properties
 *
 * @author pwinckles
 */
@Configuration
public class AuthPropsConfig extends BasePropsConfig {

    private static final String FCREPO_GROUP_AGENT_BASE_URI = "fcrepo.auth.webac.groupAgent.baseUri";
    private static final String FCREPO_USER_AGENT_BASE_URI = "fcrepo.auth.webac.userAgent.baseUri";
    private static final String FCREPO_ROOT_AUTH_ACL = "fcrepo.auth.webac.authorization";

    @Value("${" + FCREPO_ROOT_AUTH_ACL + ":#{null}}")
    private Path rootAuthAclPath;

    @Value("${" + FCREPO_USER_AGENT_BASE_URI + ":#{null}}")
    private String userAgentBaseUri;

    @Value("${" + FCREPO_GROUP_AGENT_BASE_URI + ":#{null}}")
    private String groupAgentBaseUri;

    /**
     * @return the path to the root auth acl to use instead of the default
     */
    public Path getRootAuthAclPath() {
        return rootAuthAclPath;
    }

    /**
     * @param rootAuthAclPath path to custom root auth acl
     */
    public void setRootAuthAclPath(final Path rootAuthAclPath) {
        this.rootAuthAclPath = rootAuthAclPath;
    }

    /**
     * @return the user agent base uri, if specified
     */
    public String getUserAgentBaseUri() {
        return userAgentBaseUri;
    }

    /**
     * @return the user agent base uri, if specified
     */
    public String getGroupAgentBaseUri() {
        return groupAgentBaseUri;
    }

}
