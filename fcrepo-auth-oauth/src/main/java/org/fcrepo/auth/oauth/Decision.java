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
package org.fcrepo.auth.oauth;

import java.security.Principal;

import org.apache.oltu.oauth2.rsfilter.OAuthClient;
import org.apache.oltu.oauth2.rsfilter.OAuthDecision;

/**
 * @author ajs6f
 * @date Jul 1, 2013
 */
public class Decision implements OAuthDecision {

    private OAuthClient oAuthClient;

    private Principal principal;

    private boolean isAuthorized;

    public Decision(final String client, final String principal) {
        this.oAuthClient = new OAuthClient() {

            @Override
            public String getClientId() {
                return client;
            }

        };
        this.principal = new Principal() {

            @Override
            public String getName() {
                return principal;
            }

        };
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oAuthClient;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthorized() {
        return isAuthorized;
    }

    public void setAuthorized(final boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
    }

}
