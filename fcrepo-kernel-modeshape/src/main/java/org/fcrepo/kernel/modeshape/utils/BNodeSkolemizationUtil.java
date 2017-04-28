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

package org.fcrepo.kernel.modeshape.utils;

/**
 * @author Daniel Bernstein
 * @since Apr 27, 2017
 */
public class BNodeSkolemizationUtil {

    private static final String FCREPO_BNODE_SKOLEMIZE_TO_HASH_FLAG = "fcrepo.bnode.hash-uri";

    /**
     * prevents instantiation
     */
    private BNodeSkolemizationUtil() {
    }

    /**
     * Returns true if blank nodes should follow the new strategy of skolemizing blank nodes (bnodes) to hash URIs
     * (rather than using /.well-known/genid/ - c.f. https://jira.duraspace.org/browse/FCREPO-2431).
     * 
     * @return true if fcrepo.bnode.hash-uri system property is set to "true"
     */
    public static boolean isSkolemizeToHashURIs() {
        return Boolean.valueOf(System.getProperty(FCREPO_BNODE_SKOLEMIZE_TO_HASH_FLAG, "false"));
    }

    /**
     * Sets the fcrepo.bnode.hash-uri system property.
     * @param flag to toggle this feature
     */
    public static void setSkolemizeToHashURIs(final boolean flag) {
        System.setProperty(FCREPO_BNODE_SKOLEMIZE_TO_HASH_FLAG, Boolean.toString(flag));
    }
}
