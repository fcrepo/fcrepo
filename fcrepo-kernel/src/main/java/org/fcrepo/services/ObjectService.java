
package org.fcrepo.services;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectService {

    private static final Logger logger = LoggerFactory
            .getLogger(ObjectService.class);

    @Inject
    private Repository repo;

    private static Session readOnlySession;

    public static Node
            createObjectNode(final Session session, final String path)
                    throws RepositoryException {
        return new FedoraObject(session, path).getNode();
    }

    public static Node getObjectNode(final String name)
            throws PathNotFoundException, RepositoryException {
        return readOnlySession.getNode("/objects/" + name);
    }

    @PostConstruct
    public void getSession() {
        try {
            readOnlySession = repo.login();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    public void logoutSession() {
        readOnlySession.logout();
    }

}
