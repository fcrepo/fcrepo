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
     * Namespace for the W3C WebAC vocabulary.
     */
    public static final URI WEBAC_NAMESPACE = URI.create(WEBAC_NAMESPACE_VALUE);

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
    public static final String WEBAC_MODE_APPEND_VALUE = WEBAC_NAMESPACE_VALUE + "Append";

    /**
     * Append access mode.
     */
    public static final URI WEBAC_MODE_APPEND = URI.create(WEBAC_MODE_APPEND_VALUE);

    /**
     * Control access mode.
     */
    public static final String WEBAC_MODE_CONTROL_VALUE = WEBAC_NAMESPACE_VALUE + "Control";

    /**
     * Control access mode.
     */
    public static final URI WEBAC_MODE_CONTROL = URI.create(WEBAC_MODE_CONTROL_VALUE);

    /**
     * Authorization class.
     */
    public static final String WEBAC_AUTHORIZATION_VALUE = WEBAC_NAMESPACE_VALUE + "Authorization";
    public static final URI WEBAC_AUTHORIZATION = URI.create(WEBAC_AUTHORIZATION_VALUE);

    /**
     * WebAC agent
     */
    public static final String WEBAC_AGENT_VALUE = WEBAC_NAMESPACE_VALUE + "agent";
    public static final URI WEBAC_AGENT = URI.create(WEBAC_AGENT_VALUE);

    /**
     * WebAC agentClass
     */
    public static final String WEBAC_AGENT_CLASS_VALUE = WEBAC_NAMESPACE_VALUE + "agentClass";
    public static final URI WEBAC_AGENT_CLASS = URI.create(WEBAC_AGENT_CLASS_VALUE);

    /**
     * WebAC agentGroup
     */
    public static final String WEBAC_AGENT_GROUP_VALUE = WEBAC_NAMESPACE_VALUE + "agentGroup";
    public static final URI WEBAC_AGENT_GROUP = URI.create(WEBAC_AGENT_CLASS_VALUE);

    /**
     * WebAC accessTo
     */
    public static final String WEBAC_ACCESSTO_VALUE = WEBAC_NAMESPACE_VALUE + "accessTo";
    public static final URI WEBAC_ACCESSTO = URI.create(WEBAC_ACCESSTO_VALUE);

    /**
     * WebAC accessToClass
     */
    public static final String WEBAC_ACCESSTO_CLASS_VALUE = WEBAC_NAMESPACE_VALUE + "accessToClass";
    public static final URI WEBAC_ACCESSTO_CLASS = URI.create(WEBAC_ACCESSTO_CLASS_VALUE);

    /**
     * WebAC accessControl
     */
    public static final String WEBAC_ACCESS_CONTROL_VALUE = RdfLexicon.WEBAC_ACCESS_CONTROL_VALUE;
    public static final URI WEBAC_ACCESS_CONTROL = RdfLexicon.WEBAC_ACCESS_CONTROL;

    /**
     * WebAC mode
     */
    public static final String WEBAC_MODE_VALUE = WEBAC_NAMESPACE_VALUE + "mode";
    public static final URI WEBAC_MODE = URI.create(WEBAC_MODE_VALUE);

    /**
     * FOAF Namespace
     */
    public static final String FOAF_NAMESPACE_VALUE = "http://xmlns.com/foaf/0.1/";
    public static final URI FOAF_NAMESPACE = URI.create(FOAF_NAMESPACE_VALUE);

    /**
     * FOAF Agent
     */
    public static final String FOAF_AGENT_VALUE = FOAF_NAMESPACE_VALUE + "Agent";
    public static final URI FOAF_AGENT = URI.create(FOAF_AGENT_VALUE);

    /**
     * FOAF member
     */
    public static final String FOAF_MEMBER_VALUE = FOAF_NAMESPACE_VALUE + "member";
    public static final URI FOAF_MEMBER = URI.create(FOAF_MEMBER_VALUE);

    /**
     * FOAF Group
     */
    public static final String FOAF_GROUP_VALUE = FOAF_NAMESPACE_VALUE + "Group";
    public static final URI FOAF_GROUP = URI.create(FOAF_GROUP_VALUE);

    /**
     * vCard Namespace
     */
    public static final String VCARD_NAMESPACE_VALUE = "http://www.w3.org/2006/vcard/ns#";
    public static final URI VCARD_NAMESPACE = URI.create(VCARD_NAMESPACE_VALUE);

    /**
     * vCard Group
     */
    public static final String VCARD_GROUP_VALUE = VCARD_NAMESPACE_VALUE + "Group";
    public static final URI VCARD_GROUP = URI.create(VCARD_GROUP_VALUE);

    /**
     * vCard member
     */
    public static final String VCARD_MEMBER_VALUE = VCARD_NAMESPACE_VALUE + "hasMember";
    public static final URI VCARD_MEMBER = URI.create(VCARD_MEMBER_VALUE);

    /**
     * Fedora WebAC Namespace
     */
    public static final String FEDORA_WEBAC_NAMESPACE_VALUE = "http://fedora.info/definitions/v4/webac#";
    public static final URI FEDORA_WEBAC_NAMESPACE = URI.create(FEDORA_WEBAC_NAMESPACE_VALUE);

    /**
     * Fedora ACL
     */
    public static final String FEDORA_WEBAC_ACL_VALUE = FEDORA_WEBAC_NAMESPACE_VALUE + "Acl";
    public static final URI FEDORA_WEBAC_ACL = URI.create(FEDORA_WEBAC_ACL_VALUE);

    private URIConstants() {
    }

}
