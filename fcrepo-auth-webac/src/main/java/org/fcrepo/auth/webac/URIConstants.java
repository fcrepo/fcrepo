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
package org.fcrepo.auth.webac;

import org.fcrepo.kernel.api.RdfLexicon;

import java.net.URI;

/**
 * URIs used by the WebAC module.
 *
 * @author Peter Eichman
 * @author whikloj
 * @since Aug 25, 2015
 * @see <a href="http://www.w3.org/wiki/WebAccessControl/Vocabulary">
 *      http://www.w3.org/wiki/WebAccessControl/Vocabulary</a>
 * @see <a href="http://www.w3.org/ns/auth/acl">http://www.w3.org/ns/auth/acl</a>
 * @see <a href="http://fedora.info/definitions/v4/webac">http://fedora.info/definitions/v4/webac</a>
 */
final public class URIConstants {

    /**
     * Namespace for the W3C WebAC vocabulary.
     */
    public static final String WEBAC_NAMESPACE_VALUE = RdfLexicon.WEBAC_NAMESPACE_VALUE;

    /**
     * Read access mode.
     */
    public static final String WEBAC_MODE_READ_VALUE = WEBAC_NAMESPACE_VALUE + "Read";

    /**
     * Read access mode.
     */
    public static final URI WEBAC_MODE_READ = URI.create(WEBAC_MODE_READ_VALUE);

    /**
     * Write access mode.
     */
    public static final String WEBAC_MODE_WRITE_VALUE = WEBAC_NAMESPACE_VALUE + "Write";

    /**
     * Write access mode.
     */
    public static final URI WEBAC_MODE_WRITE = URI.create(WEBAC_MODE_WRITE_VALUE);

    /**
     * Append access mode.
     */
    private static final String WEBAC_MODE_APPEND_VALUE = WEBAC_NAMESPACE_VALUE + "Append";

    /**
     * Append access mode.
     */
    public static final URI WEBAC_MODE_APPEND = URI.create(WEBAC_MODE_APPEND_VALUE);

    /**
     * Control access mode.
     */
    private static final String WEBAC_MODE_CONTROL_VALUE = WEBAC_NAMESPACE_VALUE + "Control";

    /**
     * Control access mode.
     */
    public static final URI WEBAC_MODE_CONTROL = URI.create(WEBAC_MODE_CONTROL_VALUE);

    /**
     * Authorization class.
     */
    public static final String WEBAC_AUTHORIZATION_VALUE = WEBAC_NAMESPACE_VALUE + "Authorization";

    /**
     * WebAC agent
     */
    public static final String WEBAC_AGENT_VALUE = WEBAC_NAMESPACE_VALUE + "agent";

    /**
     * WebAC agentClass
     */
    public static final String WEBAC_AGENT_CLASS_VALUE = WEBAC_NAMESPACE_VALUE + "agentClass";

    /**
     * WebAC agentGroup
     */
    public static final String WEBAC_AGENT_GROUP_VALUE = WEBAC_NAMESPACE_VALUE + "agentGroup";

    /**
     * WebAC accessTo
     */
    public static final String WEBAC_ACCESSTO_VALUE = WEBAC_NAMESPACE_VALUE + "accessTo";

    /**
     * WebAC accessToClass
     */
    public static final String WEBAC_ACCESSTO_CLASS_VALUE = WEBAC_NAMESPACE_VALUE + "accessToClass";

    /**
     * WebAC default
     */
    public static final String WEBAC_DEFAULT_VALUE = WEBAC_NAMESPACE_VALUE + "default";

    /**
     * WebAC accessControl
     */
    public static final String WEBAC_ACCESS_CONTROL_VALUE = RdfLexicon.WEBAC_ACCESS_CONTROL_VALUE;

    /**
     * WebAC mode
     */
    public static final String WEBAC_MODE_VALUE = WEBAC_NAMESPACE_VALUE + "mode";

    /**
     * WebAC AuthenticatedAgent
     */
    public static final String WEBAC_AUTHENTICATED_AGENT_VALUE = WEBAC_NAMESPACE_VALUE + "AuthenticatedAgent";

    /**
     * FOAF Namespace
     */
    private static final String FOAF_NAMESPACE_VALUE = "http://xmlns.com/foaf/0.1/";

    /**
     * FOAF Agent
     */
    public static final String FOAF_AGENT_VALUE = FOAF_NAMESPACE_VALUE + "Agent";

    /**
     * vCard Namespace
     */
    private static final String VCARD_NAMESPACE_VALUE = "http://www.w3.org/2006/vcard/ns#";

    /**
     * vCard Group
     */
    public static final String VCARD_GROUP_VALUE = VCARD_NAMESPACE_VALUE + "Group";
    public static final URI VCARD_GROUP = URI.create(VCARD_GROUP_VALUE);

    /**
     * vCard member
     */
    public static final String VCARD_MEMBER_VALUE = VCARD_NAMESPACE_VALUE + "hasMember";

    private URIConstants() {
    }

}
