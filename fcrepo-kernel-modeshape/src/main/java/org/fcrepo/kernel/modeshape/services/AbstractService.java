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
package org.fcrepo.kernel.modeshape.services;

import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaces;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.modeshape.TombstoneImpl;
import org.modeshape.jcr.api.JcrTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;


/**
 * @author bbpennel
 * @author ajs6f
 * @since Feb 20, 2014
 */
public class AbstractService {
    private final static JcrTools jcrTools = new JcrTools();

    protected static Set<String> registeredPrefixes = null;

    protected Node findOrCreateNode(final FedoraSession session,
                                    final String path,
                                    final String finalNodeType) throws RepositoryException {

        final Session jcrSession = getJcrSession(session);
        final String encodedPath = encodePath(path, session);
        final Node preexistingNode = getClosestExistingAncestor(jcrSession, encodedPath);

        if (TombstoneImpl.hasMixin(preexistingNode)) {
            throw new TombstoneException(new TombstoneImpl(preexistingNode));
        }

        final Node node = jcrTools.findOrCreateNode(jcrSession, encodedPath, NT_FOLDER, finalNodeType);

        if (node.isNew()) {
            tagHierarchyWithPairtreeMixin(preexistingNode, node);
        }

        return node;
    }

    protected Node findNode(final FedoraSession session, final String path) {
        final Session jcrSession = getJcrSession(session);
        final String encodedPath = encodePath(path, session);
        try {
            return jcrSession.getNode(encodedPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Encode colons when they are NOT preceded by a registered prefix.
     *
     * @param path the path
     * @param session a JCR session
     * @return the encoded path
     */
    public static String encodePath(final String path, final FedoraSession session) {
        return pathCoder(true, path, session);
    }

    /**
     * Decode colons when they are NOT preceded by a registered prefix.
     *
     * @param path the path
     * @param session a JCR session
     * @return the decoded path
     */
    public static String decodePath(final String path, final FedoraSession session) {
        return pathCoder(false, path, session);
    }

    /**
     * Does the actual path encoding/decoding.
     *
     * @param encode whether to encode or decode
     * @param path the path
     * @param session the JCR session
     * @return the encoded/decoded path.
     */
    private static String pathCoder(final boolean encode, final String path, final FedoraSession session) {
        final String searchString = encode ? ":" : "%3A";
        final String replaceString = encode ? "%3A" : ":";
        if (path.equals("/") || path.isEmpty() || !path.contains(searchString)) {
            // Short circuit if the path is nothing or doesn't contain the search string
            return path;
        }
        final Session jcrSession = getJcrSession(session);
        final boolean endsWithSlash = path.endsWith("/");
        final List<String> pathParts = Arrays.asList(path.split("/"));
        final List<String> newPath = new ArrayList<>();
        for (final String p : pathParts) {
            if (p.contains(searchString)) {
                final String[] prefix = p.split(searchString);
                if (!registeredPrefixes(jcrSession).contains(prefix[0])) {
                    newPath.add(p.replace(searchString, replaceString));
                    continue;
                }
            }
            newPath.add(p);
        }
        return newPath.stream().collect(Collectors.joining("/", "", endsWithSlash ? "/" : ""));
    }

    /**
     * Tag a hierarchy with {@link org.fcrepo.kernel.api.FedoraTypes#FEDORA_PAIRTREE}
     *
     * @param baseNode Top most ancestor that should not be tagged
     * @param createdNode Node whose parents should be tagged up to but not including {@code baseNode}
     * @throws RepositoryException if repository exception occurred
     */
    private static void tagHierarchyWithPairtreeMixin(final Node baseNode, final Node createdNode)
            throws RepositoryException {
        Node parent = createdNode.getParent();

        while (parent.isNew() && !parent.equals(baseNode)) {
            parent.addMixin(FEDORA_PAIRTREE);
            parent = parent.getParent();
        }
    }

    /** test node existence at path
     *
     * @param session the session
     * @param path the path
     * @return whether T exists at the given path
     */
    public boolean exists(final FedoraSession session, final String path) {
        final Session jcrSession = getJcrSession(session);
        final String encodedPath = encodePath(path, session);
        try {
            return jcrSession.nodeExists(encodedPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Get the prefixes in the JCR NamespaceRegistry
     *
     * @param session current JCR Session
     * @return Set of prefixes
     */
    protected static Set<String> registeredPrefixes(final Session session) {
        if (registeredPrefixes == null || registeredPrefixes.isEmpty()) {
            registeredPrefixes = new TreeSet<String>(getNamespaces(session).keySet());
            // Add fcr: as it is not actually registered in Modeshape.
            registeredPrefixes.add("fcr");
        }
        return registeredPrefixes;
    }
}
