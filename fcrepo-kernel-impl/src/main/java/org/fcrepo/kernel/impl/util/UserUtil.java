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

package org.fcrepo.kernel.impl.util;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

import java.net.URI;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Daniel Bernstein
 * @since Sep 25, 2017
 */
public class UserUtil {

    private static final Logger LOGGER = getLogger(UserUtil.class);

    @VisibleForTesting
    public static final String DEFAULT_USER_AGENT_BASE_URI = "info:fedora/local-user#";

    private UserUtil() {
    }

    /**
     * Returns the user agent based on the session user id.
     * @param sessionUserId the acting user's id for this session
     * @param userAgentBaseUri the user agent base uri, optional
     * @return the uri of the user agent
     */
    public static URI getUserURI(final String sessionUserId, final String userAgentBaseUri) {
        // user id could be in format <anonymous>, remove < at the beginning and the > at the end in this case.
        final String userId = (sessionUserId == null ? "anonymous" : sessionUserId).replaceAll("^<|>$", "");
        try {
            final URI uri = URI.create(userId);
            // return useId if it's an absolute URI or an opaque URI
            if (uri.isAbsolute() || uri.isOpaque()) {
                return uri;
            } else {
                return buildDefaultURI(userId, userAgentBaseUri);
            }
        } catch (final IllegalArgumentException e) {
            return buildDefaultURI(userId, userAgentBaseUri);
        }
    }

    /**
     * Build default URI with the configured base uri for agent
     * @param userId of which a URI will be created
     * @return URI
     */
    private static URI buildDefaultURI(final String userId, final String userAgentBaseUri) {
        var baseUri = userAgentBaseUri;
        // Construct the default URI for the user ID that is not a URI.
        if (isNullOrEmpty(userAgentBaseUri)) {
            // use the default local user agent base uri
            baseUri = DEFAULT_USER_AGENT_BASE_URI;
        }

        final String userAgentUri = baseUri + userId;

        LOGGER.trace("Default URI is created for user {}: {}", userId, userAgentUri);
        return URI.create(userAgentUri);
    }

}
