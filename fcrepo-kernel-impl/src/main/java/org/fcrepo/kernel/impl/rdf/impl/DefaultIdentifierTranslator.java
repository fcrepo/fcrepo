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
package org.fcrepo.kernel.impl.rdf.impl;

import javax.jcr.Session;

/**
  * A very simple {@link org.fcrepo.kernel.identifiers.IdentifierConverter} which translates JCR paths into
  * un-dereference-able Fedora subjects (by replacing JCR-specific names with
  * Fedora names). Should not be used except in "embedded" deployments in which
  * no publication of translated identifiers is expected!
 *
 * @author barmintor
 * @author ajs6f
 * @author escowles
 * @since May 15, 2013
 */
public class DefaultIdentifierTranslator extends PrefixingIdentifierTranslator {

    /**
     * Construct the graph with a placeholder resource namespace
     * @param session Session to lookup nodes
     */
    public DefaultIdentifierTranslator(final Session session) {
        super(session, "info:fedora/");
    }

}
