/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.observer;

import static com.google.common.base.Throwables.propagate;
import static org.fcrepo.kernel.utils.EventType.valueOf;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraObject;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import com.google.common.base.Predicate;

import org.slf4j.Logger;

/**
 * {@link EventFilter} that passes only events emitted from nodes with a Fedora
 * JCR type, or properties attached to them, except in the case of a node
 * removal. In that case, since we cannot test the node for its types, we assume
 * that any non-JCR namespaced node is fair game.
 *
 * @author ajs6f
 * @author barmintor
 * @date Dec 2013
 * @author eddies
 * @date Feb 7, 2013
 * @author escowles
 * @date Oct 3, 2013
 */
public class DefaultFilter implements EventFilter {

    private static final Logger LOGGER = getLogger(DefaultFilter.class);

    private Session session;

    /**
     * Default constructor.
     */
    public DefaultFilter() {
    }

    /**
     * @param session
     */
    private DefaultFilter(final Session session) {
        this.session = session;
    }

    @Override
    public Predicate<Event> getFilter(final Session session) {
        return new DefaultFilter(session);
    }

    @Override
    public boolean apply(final Event event) {
        try {
            switch (valueOf(event.getType())) {
                case NODE_REMOVED:
                    final String path = event.getPath();
                    // only propagate non-jcr node removals, a simple test, but
                    // we cannot use the predicates we use below in the absence
                    // of a node to test
                    if (!path.startsWith("jcr:", path.lastIndexOf('/') + 1)) {
                        return true;
                    }
                    break;
                default:
                    final String nodeId = event.getIdentifier();
                    final Node n = session.getNodeByIdentifier(nodeId);
                    if (isFedoraObject.apply(n) || isFedoraDatastream.apply(n)) {
                        return true;
                    }
                    break;
            }
        } catch (final PathNotFoundException e) {
            LOGGER.trace("Dropping event from outside our assigned workspace:\n", e);
            return false;
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
        return false;
    }

}
