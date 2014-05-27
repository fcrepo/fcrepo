/**
 * Copyright 2014 DuraSpace, Inc.
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
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
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
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_VERSIONS;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An extension of HttpIdentifierTranslator that includes mapping from JCR frozen nodes
 * that represent versions of versionable nodes to a URI pattern that presents
 * them as children of the original node.  The URL patterns are as follows:
 *
 * If node "node" is versionable, and has the unversionable subgraph
 * "path/to/versioned/part" and a version snapshot exists with the label
 * "label", the path to access the frozen nodes
 * is:
 * " ../node/fcr:versions/label/path/to/versioned/part".
 *
 * The "label" referred to here is either the cannonical identifier assigned
 * to the version or the user label optionally assinged at version creation
 * time.  When this class translates JCR nodes to subjects, it will always
 * return that cannonical identifier, but when mapping from graph subjects to
 * JCR nodes, either the user label or cannonical ID are supported.
 *
 * @author Mike Durbin
 * @author ajs6f
 */
public class VersionAwareHttpIdentifierTranslator extends HttpIdentifierTranslator {

    private static final Logger LOGGER = getLogger(VersionAwareHttpIdentifierTranslator.class);

    private Session internalSession;

    /**
     * Creates a GraphSubject implementation that has all of the functionality
     * of HttpIdentifierTranslator with the additional mapping versioned frozen nodes
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
    public VersionAwareHttpIdentifierTranslator(final Session session,
        final Session internalSession, final Class<?> relativeTo, final UriInfo uris) {
        super(session, relativeTo, uris);
        this.internalSession = internalSession;
    }

    @Override
    public String getPathFromGraphSubject(@NotNull final String subjectUri) throws RepositoryException {
        if (subjectUri.matches("^.*\\Q" + FCR_VERSIONS + "/\\E.+$")) {
            final Node node = getNodeFromGraphSubjectForVersionNode(subjectUri);
            if (node == null) {
                return null;
            }
            return node.getPath();
        }
        return super.getPathFromGraphSubject(subjectUri);
    }

    @Override
    public Resource getSubject(final String absPath) throws RepositoryException {
        if (absPath.contains("jcr:versionStorage")) {
            final Node probableFrozenNode = internalSession.getNode(absPath);
            if (probableFrozenNode.getPrimaryNodeType().getName().equals("nt:frozenNode")) {
                final URI result = uriBuilder.buildFromMap(getPathMapForVersionNode(probableFrozenNode));
                LOGGER.debug("Translated path {} into RDF subject {}", absPath, result);
                return createResource(result.toString());
            }
            LOGGER.debug("{} was not a frozen node... no version-specific translation required", absPath);
        }
        return super.getSubject(absPath);
    }

    /**
     * For frozen nodes (which represent version snapshots) we translate the
     * paths from their frozen storage URI to
     * "../node/fcr:versions/label/path/to/versioned/part".
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
        final String pathToVersionable = getSubjectPath(getSubject(versionableNode.getPath()));
        LOGGER.debug("frozen node found with id {}.", versionableFrozenNode.getIdentifier());
        String path =  pathToVersionable + "/" + FCR_VERSIONS + "/" + pathWithinVersionable;
        if (path.endsWith(JCR_CONTENT)) {
            path = path.replace(JCR_CONTENT, FCR_CONTENT);
        }
        return singletonMap("path", path.startsWith("/") ? path.substring(1) : path);
    }

    private static String getRelativePath(final Node node, final Node ancestor) throws RepositoryException {
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

        /*
         * Get the part of the URI after fcr:versions/ that represents the label
         * optionally followed by a path into the versioned subgraph).
         */
        final String labelAndPath
            = subjectUri.substring(subjectUri.indexOf(FCR_VERSIONS + "/") + FCR_VERSIONS.length() + 1);

        if (labelAndPath.contains("/")) {
            /*
             * The subjectUri references a node within a version snapshot
             * (identified by label) of a subgraph.
             */
            final int firstSlash = labelAndPath.indexOf('/');
            final String label = labelAndPath.substring(0, firstSlash);
            final String pathIntoVersionedGraph = labelAndPath.substring(firstSlash + 1);
            LOGGER.trace("subjectUri={}, label={}, pathIntoVersionedGraph={}", subjectUri, label,
                    pathIntoVersionedGraph);
            final Node versionedFrozenNode = getFrozenNodeByLabel(subjectUri, label);
            if (versionedFrozenNode == null) {
                // there is no version with the given label!
                return null;
            }
            return internalSession.getNode(versionedFrozenNode.getPath() + "/"
                    + pathIntoVersionedGraph.replace(FCR_CONTENT, JCR_CONTENT));
        }
        /**
         * The subjectUri references a version of a verisonable node identified
         * by label (the root of the versioned subgraph).
         */
        final String label = labelAndPath;
        LOGGER.trace("subjectUri={}, label={}", subjectUri, label);
        return getFrozenNodeByLabel(subjectUri, label);
    }

    /**
     * A private helper method that tries to look up frozen node for the given subject
     * by a label.  That label may either be one that was assigned at creation time
     * (and is a version label in the JCR sense) or a system assigned identifier that
     * was used for versions created without a label.  The current implementation
     * uses the JCR UUID for the frozen node as the system-assigned label.
     */
    private Node getFrozenNodeByLabel(final String subjectUri, final String label) throws RepositoryException {
        final String baseVersionUri = subjectUri.substring(0, subjectUri.indexOf("/" + FCR_VERSIONS));
        final String baseResourcePath = getPathFromSubject(createResource(baseVersionUri));
        try {
            final Node frozenNode = internalSession.getNodeByIdentifier(label);

            /*
             * We found a node whose identifier is the "label" for the version.  Now
             * we must do due dilligence to make sure it's a frozen node representing
             * a version of the subject node.
             */
            final Property p = frozenNode.getProperty("jcr:frozenUuid");
            if (p != null) {
                final Node subjectNode = internalSession.getNode(baseResourcePath);
                if (p.getString().equals(subjectNode.getIdentifier())) {
                    return frozenNode;
                }
            }
            /*
             * Though a node with an id of the label was found, it wasn't the
             * node we were looking for, so fall through and look for a labeled
             * node.
             */
        } catch (final ItemNotFoundException ex) {
            /*
             * the label wasn't a uuid of a frozen node but
             * instead possibly a version label.
             */
        }

        final VersionHistory hist =
            internalSession.getWorkspace().getVersionManager().getVersionHistory(baseResourcePath);
        if (hist.hasVersionLabel(label)) {
            LOGGER.debug("Found version for {} by label {}.",  subjectUri, label);
            return hist.getVersionByLabel(label).getFrozenNode();
        }
        LOGGER.warn("Unknown version {} with label or uuid {}!", subjectUri, label);
        return null;
    }
}
