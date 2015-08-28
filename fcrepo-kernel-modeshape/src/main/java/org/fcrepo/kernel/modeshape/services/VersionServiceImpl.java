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
package org.fcrepo.kernel.modeshape.services;

import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.LabelExistsVersionException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fcrepo.kernel.api.FedoraJcrTypes.VERSIONABLE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This service exposes management of node versioning.  Instead of invoking
 * the JCR VersionManager methods, this provides a level of indirection that
 * allows for special handling of features built on top of JCR such as user
 * transactions.
 * @author Mike Durbin
 */

@Component
public class VersionServiceImpl extends AbstractService implements VersionService {

    private static final Logger LOGGER = getLogger(VersionService.class);

    private static final Pattern invalidLabelPattern = Pattern.compile("[~#@*+%{}<>\\[\\]|\"^]");

    @Override
    public String createVersion(final Session session,
                              final String absPath, final String label) throws RepositoryException {
        final Node node = session.getNode(absPath);
        if (!isVersioningEnabled(node)) {
            enableVersioning(node);
        }
        return checkpoint(session, absPath, label);
    }

    @Override
    public void revertToVersion(final Session session, final String absPath,
                                final String label) throws RepositoryException {
        final Workspace workspace = session.getWorkspace();
        final Version v = getVersionForLabel(workspace, absPath, label);
        if (v == null) {
            throw new PathNotFoundException("Unknown version \"" + label + "\"!");
        }
        final VersionManager versionManager = workspace.getVersionManager();
        final Version preRevertVersion = versionManager.checkin(absPath);

        try {
            preRevertVersion.getContainingHistory().addVersionLabel(preRevertVersion.getName(),
                    getPreRevertVersionLabel(label, preRevertVersion.getContainingHistory()), false);
        } catch (final LabelExistsVersionException e) {
            // fall-back behavior is to leave an unlabeled version
        }
        versionManager.restore(v, true);
        versionManager.checkout(absPath);
    }

    /**
     * When we revert to a version, we snapshot first so that the "revert" action can be undone,
     * this method generates a label suitable for that snapshot version to make it clear why
     * it shows up in user's version history.
     * @param targetLabel
     * @param history
     * @return
     * @throws RepositoryException
     */
    private static String getPreRevertVersionLabel(final String targetLabel, final VersionHistory history)
            throws RepositoryException {
        final String baseLabel = "auto-snapshot-before-" + targetLabel;
        for (int i = 0; i < Integer.MAX_VALUE; i ++) {
            final String label = baseLabel + (i == 0 ? "" : "-" + i);
            if (!history.hasVersionLabel(label)) {
                return label;
            }
        }
        return baseLabel;
    }

    @Override
    public void removeVersion(final Session session, final String absPath,
                              final String label) throws RepositoryException {
        final Workspace workspace = session.getWorkspace();
        final Version v = getVersionForLabel(workspace, absPath, label);

        if (v == null) {
            throw new PathNotFoundException("Unknown version \"" + label + "\"!");
        } else if (workspace.getVersionManager().getBaseVersion(absPath).equals(v) ) {
            throw new VersionException("Cannot remove most recent version snapshot.");
        } else {
            // remove labels
            final VersionHistory history = v.getContainingHistory();
            final String[] versionLabels = history.getVersionLabels(v);
            for ( final String versionLabel : versionLabels ) {
                LOGGER.debug("Removing label: {}", versionLabel);
                history.removeVersionLabel( versionLabel );
            }
            history.removeVersion( v.getName() );
        }
    }


    private static Version getVersionForLabel(final Workspace workspace, final String absPath,
                                       final String label) throws RepositoryException {
        // first see if there's a version label
        final VersionHistory history = workspace.getVersionManager().getVersionHistory(absPath);

        if (history.hasVersionLabel(label)) {
            return history.getVersionByLabel(label);
        }
        return null;
    }

    private static boolean isVersioningEnabled(final Node n) throws RepositoryException {
        return n.isNodeType(VERSIONABLE) || (FedoraBinaryImpl.hasMixin(n) && isVersioningEnabled(n.getParent()));
    }

    private static void enableVersioning(final Node node) throws RepositoryException {
        node.addMixin(VERSIONABLE);

        if (FedoraBinaryImpl.hasMixin(node)) {
            node.getParent().addMixin(VERSIONABLE);
        }
        node.getSession().save();
    }

    private static String checkpoint(final Session session, final String absPath, final String label)
            throws RepositoryException {
        if (!validLabel(label)) {
            throw new VersionException("Invalid label: " + label);
        }

        LOGGER.trace("Setting version checkpoint for {}", absPath);
        final Workspace workspace = session.getWorkspace();
        final VersionManager versionManager = workspace.getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(absPath);
        if (versionHistory.hasVersionLabel(label)) {
            throw new LabelExistsVersionException("The specified label \"" + label
                    + "\" is already assigned to another version of this resource!");
        }
        final Version v = versionManager.checkpoint(absPath);
        if (v == null) {
            return null;
        }
        versionHistory.addVersionLabel(v.getName(), label, false);
        return v.getFrozenNode().getIdentifier();
    }

    private static boolean validLabel(final String label) {
        final Matcher matcher = invalidLabelPattern.matcher(label);
        return !matcher.find();
    }

}
