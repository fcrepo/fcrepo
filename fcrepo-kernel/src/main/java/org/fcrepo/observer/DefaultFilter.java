
package org.fcrepo.observer;

import static org.fcrepo.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.utils.FedoraTypesUtils.isFedoraObject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.modeshape.common.SystemFailureException;
import org.modeshape.jcr.api.Repository;

/**
 * EventFilter that passes only events emitted from nodes with
 * a Fedora JCR type.
 * 
 * @author ajs6f
 *
 */
public class DefaultFilter implements EventFilter {

    @Inject
    private Repository repository;

    // it's safe to keep the session around, because this code does not mutate
    // the state of the repository
    private Session session;

    /**
     * Filter observer events to only include events on a FedoraObject or Datastream
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
            return false; // not a node in the fedora workspace
        } catch (final LoginException e) {
            throw new SystemFailureException(e);
        } catch (final RepositoryException e) {
            throw new SystemFailureException(e);
        }
    }

    @PostConstruct
    public void acquireSession() throws LoginException, RepositoryException {
        session = repository.login();
    }

    @PreDestroy
    public void releaseSession() {
        session.logout();
    }
}