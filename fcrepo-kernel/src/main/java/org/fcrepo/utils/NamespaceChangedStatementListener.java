/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Statement;
import org.fcrepo.RdfLexicon;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date May 21, 2013
 */
public class NamespaceChangedStatementListener extends StatementListener {

    private static final Logger LOGGER =
        getLogger(NamespaceChangedStatementListener.class);

    private final NamespaceRegistry namespaceRegistry;

    /**
     * @todo Add Documentation.
     */
    public NamespaceChangedStatementListener(final Session session)
        throws RepositoryException {
        this.namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public void addedStatement(Statement s) {

        LOGGER.debug(">> added statement {}", s);
        if (!s.getPredicate().equals(RdfLexicon.HAS_NAMESPACE_PREFIX)) {
            return;
        }

        try {
            final String prefix = s.getObject().asLiteral().getString();
            final String uri = s.getSubject().asResource().getURI();
            LOGGER.debug("Registering namespace prefix {} for uri {}",
                         prefix, uri);
            namespaceRegistry.registerNamespace(prefix, uri);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public void removedStatement(Statement s) {

        LOGGER.debug(">> removed statement {}", s);

        if (!s.getPredicate().equals(RdfLexicon.HAS_NAMESPACE_PREFIX)) {
            return;
        }

        try {
            final String prefix = s.getObject().asLiteral().getString();
            final String uri = s.getSubject().asResource().getURI();
            if (namespaceRegistry.getPrefix(uri).equals(prefix)) {
                LOGGER.debug("De-registering namespace prefix {} for uri {}",
                             prefix, uri);
                namespaceRegistry.unregisterNamespace(prefix);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

}
