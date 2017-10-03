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
package org.fcrepo.integration.kernel.modeshape.services;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.VersionService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;

import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.junit.Assert.assertEquals;

/**
 * @author escowles
 * @since 2014-05-29
 */

@ContextConfiguration({"/spring-test/repo.xml"})
@Ignore("Until implemented with Memento")
public class VersionServiceImplIT extends AbstractIT {

    @Inject
    private FedoraRepository repository;

    @Inject
    NodeService nodeService;

    @Inject
    ContainerService containerService;

    @Inject
    VersionService versionService;

    private static final String LABEL = "label";
    private static final String SECOND_LABEL = "second-label";

    @Test
    public void testCreateVersion() throws RepositoryException {
        final FedoraSession session = repository.login();
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, "/" + pid, LABEL);
        session.commit();
        assertEquals(2L, countVersions(session, resource));
    }

    @Test
    public void testRemoveVersion() throws RepositoryException {
        final FedoraSession session = repository.login();
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, "/" + pid, LABEL);
        session.commit();
        assertEquals(2L, countVersions(session, resource));

        // create another version
        versionService.createVersion(session, "/" + pid, SECOND_LABEL);
        session.commit();
        assertEquals(3L, countVersions(session, resource));

        // remove the old version and make sure there two versions again
        versionService.removeVersion( session, "/" + pid,LABEL );
        session.commit();
        assertEquals(2L, countVersions(session, resource));
    }

    @Test
    public void testRevertToVersion() throws RepositoryException {
        final FedoraSession session = repository.login();
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // create a version and make sure there are 2 versions (root + created)
        final String label = versionService.createVersion(session, "/" + pid, LABEL);
        session.commit();
        assertEquals(2L, countVersions(session, resource));

        // create another version
        versionService.createVersion(session, "/" + pid, SECOND_LABEL);
        session.commit();
        assertEquals(3L, countVersions(session, resource));

        // revert to the old version and make sure there two versions again
        versionService.revertToVersion( session, "/" + pid, LABEL );
        session.commit();
        assertEquals(label, currentVersion(session,resource));
    }

    @Test( expected = RepositoryRuntimeException.class )
    public void testRevertToInvalidVersion() throws RepositoryException {
        final FedoraSession session = repository.login();
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, "/" + pid, LABEL);
        session.commit();
        assertEquals(2L, countVersions(session, resource));

        // revert to an invalid version
        versionService.revertToVersion( session, "/" + pid, "invalid-version-label" );
        session.commit();
    }

    private static String currentVersion(final FedoraSession session, final FedoraResource resource )
            throws RepositoryException {
        return getJcrSession(session).getWorkspace().getVersionManager().getBaseVersion(getJcrNode(resource).getPath())
                .getFrozenNode().getIdentifier();
    }

    private static long countVersions(final FedoraSession session, final FedoraResource resource )
            throws RepositoryException {
        final VersionHistory versions = getJcrSession(session).getWorkspace().getVersionManager().getVersionHistory(
                getJcrNode(resource).getPath() );
        return versions.getAllVersions().getSize();
    }
}
