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
package org.fcrepo.kernel.modeshape.observer;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.stream;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.observer.EventFilter;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.stream.Stream;

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

    private static final Logger LOGGER = getLogger(DefaultFilter.class);

    private static final HashSet<String> fedoraMixins =
            newHashSet(FEDORA_BINARY, FEDORA_CONTAINER, FEDORA_NON_RDF_SOURCE_DESCRIPTION, FEDORA_RESOURCE);

    @Override
    public boolean test(final Event event) {
        try {
            return getMixinTypes(event).anyMatch(fedoraMixins::contains);
        } catch (final PathNotFoundException e) {
            LOGGER.trace("Dropping event from outside our assigned workspace:\n", e);
            return false;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected static Stream<String> getMixinTypes(final Event event)
            throws PathNotFoundException, RepositoryException {
        try {
            final org.modeshape.jcr.api.observation.Event modeEvent =
                    (org.modeshape.jcr.api.observation.Event) event;
            return stream(modeEvent.getMixinNodeTypes()).map(NodeType::toString);
        } catch (final ClassCastException e) {
            throw new ClassCastException(event + " is not a Modeshape Event");
        }
    }

}
