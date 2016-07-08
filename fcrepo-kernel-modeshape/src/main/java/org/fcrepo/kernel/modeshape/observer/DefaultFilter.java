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
package org.fcrepo.kernel.modeshape.observer;

import static com.google.common.collect.ImmutableSet.of;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.ROOT;
import static org.fcrepo.kernel.modeshape.observer.FedoraEventImpl.getResourceTypes;

import javax.jcr.observation.Event;

import java.util.Set;

/**
 * {@link EventFilter} that passes only events emitted from nodes with a Fedora
 * JCR type, or properties attached to them, except in the case of a node
 * removal. In that case, since we cannot test the node for its types, we assume
 * that any non-JCR namespaced node is fair game.
 *
 * @author ajs6f
 * @author barmintor
 * @since Dec 2013
 * @author eddies
 * @since Feb 7, 2013
 * @author escowles
 * @since Oct 3, 2013
 */
public class DefaultFilter implements EventFilter {

    private static final Set<String> fedoraMixins =
            of(FEDORA_BINARY, FEDORA_CONTAINER, FEDORA_RESOURCE, ROOT);

    @Override
    public boolean test(final Event event) {
        return getResourceTypes(event).anyMatch(fedoraMixins::contains);
    }
}
