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
package org.fcrepo.kernel.impl.services;

import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.services.VersionService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import static org.fcrepo.jcr.FedoraJcrTypes.VERSIONABLE;
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

    @Override
    public String createVersion(final Session session,
                              final String absPath) throws RepositoryException {
        final Node node = session.getNode(absPath);
        if (!isVersioningEnabled(node)) {
            enableVersioning(node);
        }
        return checkpoint(session, absPath);
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
        final Version prevert = versionManager.checkin(absPath);
        versionManager.getVersionHistory(absPath).addVersionLabel(prevert.getName(), prevert.getIdentifier(), false);
        versionManager.restore(v, true);
        versionManager.checkout(absPath);
    }

    @Override
    public void removeVersion(final Session session, final String absPath,
                              final String label) throws RepositoryException {
        final Workspace workspace = session.getWorkspace();
        final Version v = getVersionForLabel(workspace, absPath, label);

        if (v == null) {
            throw new PathNotFoundException("Unknown version \"" + label + "\"!");
        } else if (workspace.getVersionManager().getBaseVersion(absPath).equals(v) ) {
            throw new VersionException("Cannot remove current version");
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
        // there was no version with the given JCR Version Label, check to see if
        // there's a version whose UUID is equal to the label
        final VersionIterator versionIt = history.getAllVersions();
        if (versionIt == null) {
            return null;
        }
        while (versionIt.hasNext()) {
            final Version v = versionIt.nextVersion();
            if (v.getFrozenNode().getIdentifier().equals(label)) {
                return v;
            }
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

    private static String checkpoint(final Session session, final String absPath) throws RepositoryException {
        LOGGER.trace("Setting implicit version checkpoint set for {}", absPath);
        final Workspace workspace = session.getWorkspace();
        final Version v = workspace.getVersionManager().checkpoint(absPath);
        return ( v == null ) ? null : v.getFrozenNode().getIdentifier();
    }

}
