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
package org.fcrepo.kernel.modeshape.identifiers;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.validatePath;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;

import org.fcrepo.kernel.api.exception.IdentifierConversionException;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.NonRdfSourceDescriptionImpl;
import org.fcrepo.kernel.modeshape.TombstoneImpl;
import org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils;

import org.slf4j.Logger;

/**
 * Convert between Internal Paths and Nodes
 * Internal Paths are convertible to JCR paths by negotiating
 * the signal suffixes (fcr:metadata, fcr:tombstone)
 * @author barmintor
 */
public class InternalPathToNodeConverter extends IdentifierConverter<String,Node> {

    private static final Logger LOGGER = getLogger(InternalPathToNodeConverter.class);

    private final Session session;

    /**
     * Create a new identifier converter within the given session with the given URI template
     * @param session the session
     */
    public InternalPathToNodeConverter(final Session session) {
        this.session = session;
    }

    @Override
    public Node apply(final String path) {
        try {
            final String realPath = trimSignalSuffixes(getVersionPath(path));
            if (path != null) {
                final boolean metadata = path.endsWith("/" + FCR_METADATA);
                final Node node = getNode(realPath);

                if (!metadata && NonRdfSourceDescriptionImpl.hasMixin(node)) {
                    return node.getNode("jcr:content");
                }
                return node;
            }
            throw new IdentifierConversionException("Asked to translate a null path");
        } catch (final RepositoryException e) {
            final String realPath = trimSignalSuffixes(getVersionPath(path));

            validatePath(session, realPath);

            if ( e instanceof PathNotFoundException ) {
                try {
                    final Node preexistingNode = getClosestExistingAncestor(session, realPath);
                    if (TombstoneImpl.hasMixin(preexistingNode)) {
                        throw new TombstoneException(new TombstoneImpl(preexistingNode));
                    }
                } catch (final RepositoryException inner) {
                    LOGGER.debug("Error checking for parent tombstones", inner);
                }
            }
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Remove the suffixes for magic paths
     * @param path
     * @return
     */
    public static String trimSignalSuffixes(final String path) {
        return removeEnd(removeEnd(removeEnd(path,"/fcr:metadata"), "/fcr:tombstone"), "/jcr:content");
    }

    @Override
    public String toDomain(final Node resource) {
        String internalPath = getPathOrVersionPath(resource);
        if (NonRdfSourceDescriptionImpl.hasMixin(resource)) {
            internalPath = internalPath + "/fcr:metadata";
        } else if (FedoraBinaryImpl.hasMixin(resource)) {
            internalPath = removeEnd(internalPath, "/jcr:content");
        } else if (TombstoneImpl.hasMixin(resource)) {
            internalPath = internalPath + "/fcr:tombstone";
        }
        return (internalPath == null) ? "" : internalPath;
    }

    @Override
    public boolean inDomain(final String resource) {
        return resource != null;
    }

    @Override
    public String asString(final String resource) {
        try {
            return apply(resource).getPath();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private String getVersionPath(final String path) {
        if (path.contains(FCR_VERSIONS)) {
            final String[] split = path.split("/" + FCR_VERSIONS + "/", 2);
            final String versionedPath = split[0];
            final String versionAndPathIntoVersioned = split[1];
            final String[] split1 = versionAndPathIntoVersioned.split("/", 2);
            final String version = split1[0];

            final String pathIntoVersioned;
            if (split1.length > 1) {
                pathIntoVersioned = split1[1];
            } else {
                pathIntoVersioned = "";
            }

            final String frozenPath = getFrozenNodePathByLabel(versionedPath, version);

            if (pathIntoVersioned.isEmpty()) {
                return frozenPath;
            } else if (frozenPath != null) {
                return frozenPath + "/" + pathIntoVersioned;
            }
        }
        return path;
    }

    /**
     * A private helper method that tries to look up frozen node for the given subject
     * by a label.  That label may either be one that was assigned at creation time
     * (and is a version label in the JCR sense) or a system assigned identifier that
     * was used for versions created without a label.  The current implementation
     * uses the JCR UUID for the frozen node as the system-assigned label.
     */
    private String getFrozenNodePathByLabel(final String baseResourcePath, final String label) {
        try {
            final Node n = getNode(baseResourcePath, label);

            if (n != null) {
                return n.getPath();
            }

             /*
             * Though a node with an id of the label was found, it wasn't the
             * node we were looking for, so fall through and look for a labeled
             * node.
             */
            final VersionHistory hist =
                    session.getWorkspace().getVersionManager().getVersionHistory(baseResourcePath);
            if (hist.hasVersionLabel(label)) {
                LOGGER.debug("Found version for {} by label {}.", baseResourcePath, label);
                return hist.getVersionByLabel(label).getFrozenNode().getPath();
            }
            LOGGER.warn("Unknown version {} with label or uuid {}!", baseResourcePath, label);
            throw new PathNotFoundException("Unknown version " + baseResourcePath
                    + " with label or uuid " + label);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }


    private Node getNode(final String baseResourcePath, final String label) throws RepositoryException {
        try {
            final Node frozenNode = session.getNodeByIdentifier(label);

            /*
             * We found a node whose identifier is the "label" for the version.  Now
             * we must do due diligence to make sure it's a frozen node representing
             * a version of the subject node.
             */
            final Property p = frozenNode.getProperty("jcr:frozenUuid");
            if (p != null) {
                final Node subjectNode = session.getNode(baseResourcePath);
                if (p.getString().equals(subjectNode.getIdentifier())) {
                    return frozenNode;
                }
            }

        } catch (final ItemNotFoundException ex) {
            /*
             * the label wasn't a uuid of a frozen node but
             * instead possibly a version label.
             */
        }
        return null;
    }

    private Node getNode(final String path) throws RepositoryException {
        try {
            return session.getNode(path);
        } catch (IllegalArgumentException ex) {
            throw new InvalidResourceIdentifierException("Illegal path: " + path);
        }
    }

    private String getPathOrVersionPath(final Node resource) {
        if (FedoraTypesUtils.isFrozenNode.test(resource)) {
            // the versioned resource we're in
            final Node versionableFrozenResource = FedoraResourceImpl.getVersionedAncestor(resource);

            // the unfrozen equivalent for the versioned resource
            final Node unfrozenVersionableNode = FedoraResourceImpl.getUnfrozenNode(resource);

            // the label for this version
            final String versionLabel = FedoraResourceImpl.getVersionLabelOfFrozenResource(versionableFrozenResource);

            // the path to this resource within the versioning tree
            final String pathWithinVersionable;

            if (!resource.equals(versionableFrozenResource)) {
                pathWithinVersionable = getRelativePath(resource, versionableFrozenResource);
            } else {
                pathWithinVersionable = "";
            }

            // and, finally, the path we want to expose in the URI
            final String path = trimSignalSuffixes(getPath(unfrozenVersionableNode))
                    + "/" + FCR_VERSIONS
                    + (versionLabel != null ? "/" + versionLabel : "")
                    + pathWithinVersionable;
            return path.startsWith("/") ? path : "/" + path;
        }
        return getPath(resource);
    }

    private static String getPath(final Node node) {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private static String getRelativePath(final Node child, final Node ancestor) {
        try {
            return child.getPath().substring(ancestor.getPath().length());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

}
