package org.fcrepo.utils;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Statement;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.slf4j.LoggerFactory.getLogger;

public class NamespaceChangedStatementListener extends StatementListener {

    private static final Logger LOGGER = getLogger(NamespaceChangedStatementListener.class);

    private final NamespaceRegistry namespaceRegistry;

    public NamespaceChangedStatementListener(final Session session) throws RepositoryException {
        this.namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
    }

    @Override
    public void addedStatement(Statement s) {

        LOGGER.debug(">> added statement {}", s);

        if (!s.getPredicate().asResource().getURI().equals(JcrRdfTools.HAS_NAMESPACE_PREDICATE)) {
             return;
        }

        try {
            final String prefix = s.getObject().asLiteral().getString();
            final String uri = s.getSubject().asResource().getURI();
            LOGGER.debug("Registering namespace prefix {} for uri {}", prefix, uri);
            namespaceRegistry.registerNamespace(prefix, uri);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void removedStatement(Statement s) {

        LOGGER.debug(">> removed statement {}", s);

        if (!s.getPredicate().asResource().getURI().equals(JcrRdfTools.HAS_NAMESPACE_PREDICATE)) {
            return;
        }

        try {
            final String prefix = s.getObject().asLiteral().getString();
            final String uri = s.getSubject().asResource().getURI();
            if (namespaceRegistry.getPrefix(uri).equals(prefix)) {
                LOGGER.debug("De-registering namespace prefix {} for uri {}", prefix, uri);
                namespaceRegistry.unregisterNamespace(prefix);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
