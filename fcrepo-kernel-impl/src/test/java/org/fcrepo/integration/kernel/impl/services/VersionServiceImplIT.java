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
package org.fcrepo.integration.kernel.impl.services;

import static java.util.Collections.singleton;

import static org.junit.Assert.assertEquals;

import org.fcrepo.integration.kernel.impl.AbstractIT;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.VersionService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collection;

import javax.inject.Inject;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;

/**
 * @author escowles
 * @since 2014-05-29
 */

@ContextConfiguration({"/spring-test/repo.xml"})
public class VersionServiceImplIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    NodeService nodeService;

    @Inject
    ObjectService objectService;

    @Inject
    VersionService versionService;

/*
  versionService.createVersion( workspace, paths )
  versionService.revertToVersion( workspace, path, label )
  versionService.removeVersion( workspace, path, label )
*/

    @Test
    public void testCreateVersion() throws RepositoryException {
        final Session session = repository.login();
        final String pid = getTestObjIdentifier();
        final FedoraResource resource = nodeService.findOrCreateObject(session, "/" + pid);
        session.save();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session.getWorkspace(), singleton("/" + pid));
        session.save();
        assertEquals(2L, countVersions(session, resource));
    }

    @Test
    public void testRemoveVersion() throws RepositoryException {
        final Session session = repository.login();
        final String pid = getTestObjIdentifier();
        final FedoraResource resource = nodeService.findOrCreateObject(session, "/" + pid);
        session.save();

        // create a version and make sure there are 2 versions (root + created)
        final Collection<String> label = versionService.createVersion(session.getWorkspace(), singleton("/" + pid));
        session.save();
        assertEquals(2L, countVersions(session, resource));

        // create another version
        versionService.createVersion(session.getWorkspace(), singleton("/" + pid));
        session.save();
        assertEquals(3L, countVersions(session, resource));

        // remove the old version and make sure there two versions again
        versionService.removeVersion( session.getWorkspace(), "/" + pid, label.iterator().next() );
        session.save();
        assertEquals(2L, countVersions(session, resource));
    }

    @Test
    public void testRevertToVersion() throws RepositoryException {
        final Session session = repository.login();
        final String pid = getTestObjIdentifier();
        final FedoraResource resource = nodeService.findOrCreateObject(session, "/" + pid);
        session.save();

        // create a version and make sure there are 2 versions (root + created)
        final Collection<String> label = versionService.createVersion(session.getWorkspace(), singleton("/" + pid));
        session.save();
        assertEquals(2L, countVersions(session, resource));

        // create another version
        versionService.createVersion(session.getWorkspace(), singleton("/" + pid));
        session.save();
        assertEquals(3L, countVersions(session, resource));

        // revert to the old version and make sure there two versions again
        versionService.revertToVersion( session.getWorkspace(), "/" + pid, label.iterator().next() );
        session.save();
        assertEquals(label.iterator().next(), currentVersion(session,resource));
    }

    @Test( expected = PathNotFoundException.class )
    public void testRevertToInvalidVersion() throws RepositoryException {
        final Session session = repository.login();
        final String pid = getTestObjIdentifier();
        final FedoraResource resource = nodeService.findOrCreateObject(session, "/" + pid);
        session.save();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session.getWorkspace(), singleton("/" + pid));
        session.save();
        assertEquals(2L, countVersions(session, resource));

        // revert to an invalid version
        versionService.revertToVersion( session.getWorkspace(), "/" + pid, "invalid-version-label" );
        session.save();
    }

    @Test( expected = PathNotFoundException.class )
    public void testRemoveInvalidVersion() throws RepositoryException {
        final Session session = repository.login();
        final String pid = getTestObjIdentifier();
        final FedoraResource resource = nodeService.findOrCreateObject(session, "/" + pid);
        session.save();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session.getWorkspace(), singleton("/" + pid));
        session.save();
        assertEquals(2L, countVersions(session, resource));

        // revert to an invalid version
        versionService.removeVersion( session.getWorkspace(), "/" + pid, "invalid-version-label" );
        session.save();
    }

    private static String currentVersion( final Session session, final FedoraResource resource )
        throws RepositoryException {
        return session.getWorkspace().getVersionManager().getBaseVersion(resource.getNode().getPath())
            .getFrozenNode().getIdentifier();
    }

    private static long countVersions( final Session session, final FedoraResource resource )
        throws RepositoryException {
        final VersionHistory versions = session.getWorkspace().getVersionManager().getVersionHistory(
            resource.getNode().getPath() );
        return versions.getAllVersions().getSize();
    }
}
