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
package org.fcrepo.kernel.observer;

import static com.google.common.base.Throwables.propagate;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraObject;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import com.google.common.base.Function;
import org.slf4j.Logger;

/**
 * EventFilter that passes only events emitted from nodes with
 * a Fedora JCR type, or properties attached to them.
 *
 * @author ajs6f
 * @author barmintor
 * @date Dec 2013
 *
 * @author eddies
 * @date Feb 7, 2013
 *
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
    public Function<Event, Event> getFilter(final Session session) {
        return new DefaultFilter(session);
    }

    @Override
    public Event apply(final Event event) {
        try {
            String nPath = event.getPath();
            final int nType = event.getType();
            switch (nType) {
                case NODE_ADDED:
                    break;
                case NODE_REMOVED:
                    return event;
                case PROPERTY_ADDED:
                    nPath = nPath.substring(0, nPath.lastIndexOf('/'));
                    break;
                case PROPERTY_REMOVED:
                    nPath = nPath.substring(0, nPath.lastIndexOf('/'));
                    break;
                case PROPERTY_CHANGED:
                    nPath = nPath.substring(0, nPath.lastIndexOf('/'));
                    break;
                case NODE_MOVED:
                    break;
                default:
                    return null;
            }

            final Node n = session.getNode(nPath);
            if (isFedoraObject.apply(n)) {
                return new FedoraEvent(event, FEDORA_OBJECT);
            }
            if (isFedoraDatastream.apply(n)) {
                return new FedoraEvent(event, FEDORA_DATASTREAM);
            }
        } catch (final PathNotFoundException e) {
            LOGGER.trace("Dropping event from outside our assigned workspace", e);
            return null;
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
        return null;
    }

}
