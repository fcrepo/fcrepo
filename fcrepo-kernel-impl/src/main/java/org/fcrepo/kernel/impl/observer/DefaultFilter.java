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
package org.fcrepo.kernel.impl.observer;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.fcrepo.kernel.observer.EventFilter;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;

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

    private static final Function<NodeType, String> nodetype2string = new Function<NodeType, String>() {
        @Override
        public String apply(final NodeType input) {
            return input.getName();
        }
    };

    /**
     * Default constructor.
     */
    public DefaultFilter() {
    }

    /**
     * @param session
     */
    private DefaultFilter(final Session session) {
    }

    @Override
    public Predicate<Event> getFilter(final Session session) {
        return new DefaultFilter(session);
    }

    @Override
    public boolean apply(final Event event) {
        try {
            final org.modeshape.jcr.api.observation.Event modeEvent = getJcr21Event(event);

            final List<NodeType> nodeTypes = ImmutableList.copyOf(modeEvent.getMixinNodeTypes());
            final Collection<String> mixinTypes = ImmutableSet.copyOf(transform(nodeTypes, nodetype2string));
            return mixinTypes.contains(FEDORA_RESOURCE)
                    || mixinTypes.contains(FEDORA_BINARY)
                    || mixinTypes.contains(FEDORA_NON_RDF_SOURCE_DESCRIPTION)
                    || mixinTypes.contains(FEDORA_CONTAINER);
        } catch (final PathNotFoundException e) {
            LOGGER.trace("Dropping event from outside our assigned workspace:\n", e);
            return false;
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    private static org.modeshape.jcr.api.observation.Event getJcr21Event(final Event event) {
        try {
            return (org.modeshape.jcr.api.observation.Event) event;
        } catch (final ClassCastException e) {
            throw new ClassCastException(event + " is not a Modeshape Event");
        }
    }

}
