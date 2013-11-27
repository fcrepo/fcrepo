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
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraObjectOrDatastream;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.modeshape.jcr.api.Repository;

/**
 * EventFilter that passes only events emitted from nodes with
 * a Fedora JCR type, or properties attached to them.
 *
 * @author eddies
 * @date Feb 7, 2013
 *
 * @author escowles
 * @date Oct 3, 2013
 */
public class DefaultFilter implements EventFilter {

    @Inject
    private Repository repository;

    /**
     * Filter observer events to only include events on a FedoraObject or
     * Datastream, or properties of an FedoraObject or Datastream.
     *
     * @param event the original event
     * @return
     */
    @Override
    public boolean apply(final Event event) {
        Session session = null;
        try {
            String nPath = event.getPath();
            int nType = event.getType();
            switch(nType) {
                case NODE_ADDED:
                    break;
                case NODE_REMOVED:
                    return true;
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
                    return false;
            }

            session = repository.login();
            final Node n = session.getNode(nPath);
            return isFedoraObjectOrDatastream.apply(n);
        } catch (final PathNotFoundException e) {
            // not a node in the fedora workspace
            return false;
        } catch (final RepositoryException e) {
            throw propagate(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
