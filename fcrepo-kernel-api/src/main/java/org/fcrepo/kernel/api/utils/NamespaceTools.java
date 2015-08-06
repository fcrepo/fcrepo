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
package org.fcrepo.kernel.api.utils;

import java.util.Objects;
import java.util.function.Function;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.modeshape.jcr.api.NamespaceRegistry;

/**
 * Tools for working with the JCR Namespace Registry
 * (wrapping some non-standard Modeshape machinery)
 * @author Benjamin Armintor
 * @since May 13, 2013
 */
public final class NamespaceTools {

    private NamespaceTools() {
    }

    /**
     * We need the Modeshape NamespaceRegistry, because it allows us to register
     * anonymous namespaces.
     */
    public static Function<Node, NamespaceRegistry> getNamespaceRegistry = new Function<Node, NamespaceRegistry>() {
        @Override
        public NamespaceRegistry apply(final Node n) {
            try {
                Objects.requireNonNull(n, "null has no Namespace Registry associated with it!");
                return (org.modeshape.jcr.api.NamespaceRegistry)n.getSession().getWorkspace().getNamespaceRegistry();
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }

    };

    /**
     * Return the javax.jcr.NamespaceRegistry associated with the arg session.
     *
     * @param session containing the NamespaceRegistry
     * @return NamespaceRegistry
     */
    public static javax.jcr.NamespaceRegistry getNamespaceRegistry(final Session session) {
        final javax.jcr.NamespaceRegistry namespaceRegistry;
        try {
            namespaceRegistry =
                    session.getWorkspace().getNamespaceRegistry();
            Objects.requireNonNull(namespaceRegistry,
                    "Couldn't find namespace registry in repository!");
            return namespaceRegistry;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Validate resource path for unregistered namespace prefixes
     *
     * @param session the JCR session to use
     * @param path the absolute path to the object
     * @throws org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException on unregistered namespaces
     * @throws org.fcrepo.kernel.api.exception.RepositoryRuntimeException if repository runtime exception occurred
     */
    public static void validatePath(final Session session, final String path) {

        final javax.jcr.NamespaceRegistry namespaceRegistry = getNamespaceRegistry(session);

        final String relPath = path.replaceAll("^/+", "").replaceAll("/+$", "");
        final String[] pathSegments = relPath.split("/");
        for (final String segment : pathSegments) {
            if (segment.length() > 0 && segment.contains(":") &&
                    !segment.substring(0, segment.indexOf(':')).equals("fedora")) {
                final String prefix = segment.substring(0, segment.indexOf(':'));
                if (prefix.length() == 0) {
                    throw new FedoraInvalidNamespaceException(
                            String.format("Unable to identify namespace for (%s)", segment));
                }
                try {
                    namespaceRegistry.getURI(prefix);
                } catch (final NamespaceException e) {
                    throw new FedoraInvalidNamespaceException(
                            String.format("The namespace prefix (%s) has not been registered", prefix), e);
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        }
    }

}
