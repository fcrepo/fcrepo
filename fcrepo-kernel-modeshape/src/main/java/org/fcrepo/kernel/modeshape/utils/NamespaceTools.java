/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.utils;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Objects.requireNonNull;
import static java.util.Arrays.stream;
import static javax.jcr.NamespaceRegistry.PREFIX_EMPTY;
import static javax.jcr.NamespaceRegistry.PREFIX_JCR;
import static javax.jcr.NamespaceRegistry.PREFIX_MIX;
import static javax.jcr.NamespaceRegistry.PREFIX_NT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * Tools for working with the JCR Namespace Registry
 * @author Benjamin Armintor
 * @author acoburn
 * @author ajs6f
 * @since May 13, 2013
 */
public final class NamespaceTools {

    private NamespaceTools() {
    }

    // Internal namespaces defined in the modeshape CND file
    private static final Set<String> INTERNAL_PREFIXES = of(PREFIX_EMPTY, PREFIX_JCR, PREFIX_MIX, PREFIX_NT,
            "mode", "sv", "image");

    /**
     * Return the {@link NamespaceRegistry} associated with the arg session.
     *
     * @param session containing the NamespaceRegistry
     * @return NamespaceRegistry
     */
    public static NamespaceRegistry getNamespaceRegistry(final Session session) {
        try {
            return requireNonNull(session.getWorkspace().getNamespaceRegistry(),
                    "Couldn't find namespace registry in repository!");
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

        final NamespaceRegistry namespaceRegistry = getNamespaceRegistry(session);
        final String[] pathSegments = path.replaceAll("^/+", "").replaceAll("/+$", "").split("/");
        for (final String segment : pathSegments) {
            final int colonPosition = segment.indexOf(':');
            if (segment.length() > 0 && colonPosition > -1) {
                final String prefix = segment.substring(0, colonPosition);
                if (!prefix.equals("fedora")) {
                    if (prefix.length() == 0) {
                        throw new FedoraInvalidNamespaceException("Empty namespace in " + segment);
                    }
                    try {
                        namespaceRegistry.getURI(prefix);
                    } catch (final NamespaceException e) {
                        throw new FedoraInvalidNamespaceException("Prefix " + prefix + " has not been registered", e);
                    } catch (final RepositoryException e) {
                        throw new RepositoryRuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Retrieve the namespaces as a Map
     *
     * @param session the JCR session to use
     * @return a mapping of the prefix to URI
     */
    public static Map<String, String> getNamespaces(final Session session) {
        final NamespaceRegistry registry = getNamespaceRegistry(session);
        final Map<String, String> namespaces = new HashMap<>();

        try {
            stream(registry.getPrefixes()).filter(internalPrefix.negate()).forEach(x -> {
                try {
                    namespaces.put(x, registry.getURI(x));
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            });
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return namespaces;
    }

    private static Predicate<String> internalPrefix = INTERNAL_PREFIXES::contains;
}
