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
package org.fcrepo.utils;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.RdfLexicon;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Listener that maps changes to actions in the JCR namespace registry
 */
public class NamespaceChangedStatementListener extends StatementListener {

    private static final Logger LOGGER =
        getLogger(NamespaceChangedStatementListener.class);

    private final NamespaceRegistry namespaceRegistry;

    /**
     * Use the given session to perform namespace changes
     * @param session
     * @throws RepositoryException
     */
    public NamespaceChangedStatementListener(final Session session)
        throws RepositoryException {
        this.namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
    }

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
