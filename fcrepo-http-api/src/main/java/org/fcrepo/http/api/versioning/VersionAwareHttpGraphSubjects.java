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
package org.fcrepo.http.api.versioning;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Collections.singletonMap;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An extension of HttpGraphSubjects that includes mapping from JCR frozen nodes
 * that represent versions of versionable nodes to a URI pattern that presents
 * them as children of the original node.  The URL patterns are as follows:
 *
 * If node "node" is versionable, and has the unversionable subgraph
 * "path/to/versioned/part" and a version snapshot exists with the UUID of
 * "uuid-of-versionable-frozen-node", the path to access the frozen nodes
 * is:
 * " ../node/fcr:versions/uuid-of-versionable-frozen-node/path/to/versioned/part".
 *
 * @author Mike Durbin
 */
public class VersionAwareHttpGraphSubjects extends HttpGraphSubjects {

    private static final Logger LOGGER = getLogger(VersionAwareHttpGraphSubjects.class);

    private Session internalSession;

    /**
     * Creates a GraphSubject implementation that has all of the functionality
     * of HttpGraphSubjects with the additional mapping versioned frozen nodes
     * to URIs that appear in the hierarchy of the original node.
     *
     * This mapping can be heavyweight in that it has to do version lookups and
     * other expensive operations for nodes that appear to be historic versions.
     *
     * @param session the current session (may contain transaction information)
     * @param internalSession a session with read access to all content
     * @param relativeTo a class whose path annotation will be used as the
     *                   base for all relative paths
     * @param uris a UriInfo implementation with information about the request
     *             URI.
     */
    public VersionAwareHttpGraphSubjects(Session session, Session internalSession, Class<?> relativeTo, UriInfo uris) {
        super(session, relativeTo, uris);
        this.internalSession = internalSession;
    }

    @Override
    public String getPathFromGraphSubject(@NotNull final String subjectUri) throws RepositoryException {
        if (subjectUri.matches("^.*\\Qfcr:versions/\\E.+$")) {
            Node node = getNodeFromGraphSubjectForVersionNode(subjectUri);
            if (node == null) {
                return null;
            } else {
                return node.getPath();
            }
        } else {
            return super.getPathFromGraphSubject(subjectUri);
        }
    }

    @Override
    public Resource getGraphSubject(final String absPath) throws RepositoryException {
        if (absPath.contains("jcr:versionStorage")) {
            Node probableFrozenNode = internalSession.getNode(absPath);
            if (probableFrozenNode.getPrimaryNodeType().getName().equals("nt:frozenNode")) {
                URI result = nodesBuilder.buildFromMap(getPathMapForVersionNode(probableFrozenNode));
                LOGGER.debug("Translated path {} into RDF subject {}", absPath, result);
                return createResource(result.toString());
            } else {
                LOGGER.debug("{} was not a frozen node... no version-specific translation required", absPath);
            }
        }
        return super.getGraphSubject(absPath);
    }

    @Override
    public Resource getGraphSubject(final Node node) throws RepositoryException {
        if (node.getPrimaryNodeType().getName().equals("nt:frozenNode")) {
            URI result = nodesBuilder.buildFromMap(getPathMapForVersionNode(node));
            LOGGER.debug("Translated node {} into RDF subject {}", node, result);
            return createResource(result.toString());
        }
        return super.getGraphSubject(node);
    }

    /**
     * For frozen nodes (which represent version snapshots) we translate the
     * paths from their frozen storage URI to
     * "../node/fcr:versions/uuid-of-versionable-frozen-node/path/to/versioned/part".
     * @see #getNodeFromGraphSubjectForVersionNode
     * @param frozenNode an nt:frozenNode whose fedora-specific URI path is to be
     *             retrieved
     * @return a map with a single entry with the key "path" and the value
     *         equal to the fedora path for the version node.
     * @throws NullPointerException if session is null
     */
    private Map<String,String> getPathMapForVersionNode(final Node frozenNode)
        throws RepositoryException {
        Node versionableFrozenNode = frozenNode;
        Node versionableNode = internalSession.getNodeByIdentifier(
                versionableFrozenNode.getProperty("jcr:frozenUuid").getString());
        String pathWithinVersionable = "";
        while (!versionableNode.isNodeType("mix:versionable")) {
            LOGGER.debug("node {} is not versionable, checking parent...", versionableNode.getIdentifier());
            pathWithinVersionable = "/" + getRelativePath(versionableNode,
                    versionableNode.getParent()) + pathWithinVersionable;
            versionableFrozenNode = versionableFrozenNode.getParent();
            versionableNode = internalSession.getNodeByIdentifier(
                    versionableFrozenNode.getProperty("jcr:frozenUuid").getString());
        }

        pathWithinVersionable = versionableFrozenNode.getIdentifier()
                + (pathWithinVersionable.length() > 0 ? pathWithinVersionable : "");
        String pathToVersionable = versionableNode.getPath();
        LOGGER.debug("frozen node found with id {}.", versionableFrozenNode.getIdentifier());
        String path =  pathToVersionable + "/fcr:versions/" + pathWithinVersionable;
        if (path.endsWith(JCR_CONTENT)) {
            path = path.replace(JCR_CONTENT, FCR_CONTENT);
        }
        return singletonMap("path", path.startsWith("/") ? path.substring(1) : path);
    }

    private String getRelativePath(Node node, Node ancestor) throws RepositoryException {
        return node.getPath().substring(ancestor.getPath().length() + 1);
    }

    /**
     * Gets the frozen node in the system for the version described as the
     * passed subject.
     * @see #getPathMapForVersionNode
     * @param subjectUri a subject containing the fedora identifier as a URI
     * @return the node that represents the version snapshot of the subject.
     * @throws RepositoryException
     * @throws NullPointerException if session is null
     */
    public Node getNodeFromGraphSubjectForVersionNode(final String subjectUri)
        throws RepositoryException {
        final String versionPath = subjectUri.substring(subjectUri.indexOf("/fcr:versions") + 14);
        final int firstSlash = versionPath.indexOf('/');
        final String uuid = firstSlash == -1 ? versionPath : versionPath.substring(0, firstSlash);
        final String pathIntoVersionedGraph = firstSlash == -1 ? null : versionPath.substring(firstSlash + 1);
        final Node versionedFrozenNode = getFrozenNodeByUUIDOrLabel(subjectUri, uuid);
        if (versionedFrozenNode == null) {
            return null;
        } else {
            if (pathIntoVersionedGraph == null) {
                return versionedFrozenNode;
            } else {
                return internalSession.getNode(versionedFrozenNode.getPath() + "/"
                        + pathIntoVersionedGraph.replace(FCR_CONTENT, JCR_CONTENT));
            }
        }
    }

    private Node getFrozenNodeByUUIDOrLabel(String subjectUri, String uuid) throws RepositoryException {
        try {
            LOGGER.debug("Found version for {} by uuid {}.", subjectUri, uuid);
            return internalSession.getNodeByIdentifier(uuid);
        } catch (ItemNotFoundException ex) {
            // the uuid-of-frozen-node wasn't a uuid of a frozen node but
            // instead possibly a version label
            final String baseResourceUri = subjectUri.substring(0,
                    subjectUri.indexOf("/fcr:versions"));
            VersionHistory hist = internalSession.getWorkspace().getVersionManager()
                    .getVersionHistory(
                            getPathFromGraphSubject(baseResourceUri));
            if (hist.hasVersionLabel(uuid)) {
                LOGGER.debug("Found version for {} by label {}.",
                        subjectUri, uuid);
                return hist.getVersionByLabel(uuid).getFrozenNode();
            } else {
                LOGGER.warn("Unknown version {} with label or uuid {}!",
                        subjectUri, uuid);
                return null;
            }
        }
    }
}
