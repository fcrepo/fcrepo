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
package org.fcrepo.observer;

import static com.google.common.base.Throwables.propagate;
import static org.fcrepo.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.utils.FedoraTypesUtils.isFedoraObject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.modeshape.jcr.api.Repository;

/**
 * EventFilter that passes only events emitted from nodes with
 * a Fedora JCR type.
 *
 * @author eddies
 * @date Feb 7, 2013
 */
public class DefaultFilter implements EventFilter {

    @Inject
    private Repository repository;

    // it's safe to keep the session around, because this code does not mutate
    // the state of the repository
    private Session session;

    /**
     * Filter observer events to only include events on a FedoraObject or
     * Datastream
     *
     * @param event the original event
     * @return
     */
    @Override
    public boolean apply(final Event event) {

        try {
            final Node resource = session.getNode(event.getPath());
            return isFedoraObject.apply(resource) ||
                isFedoraDatastream.apply(resource);

        } catch (final PathNotFoundException e) {
            // not a node in the fedora workspace
            return false;
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    @PostConstruct
    public void acquireSession() throws RepositoryException {
        session = repository.login();
    }

    @PreDestroy
    public void releaseSession() {
        session.logout();
    }
}
