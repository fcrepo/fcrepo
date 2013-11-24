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

package org.fcrepo.integration.kernel.services;

import static com.google.common.io.Files.createTempDir;
import static org.fcrepo.kernel.RdfLexicon.HAS_NAMESPACE_PREFIX;
import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.inject.Inject;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.AbstractIT;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.ObjectService;
import org.junit.Test;
import org.modeshape.jcr.api.Problems;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.UpdateAction;

@ContextConfiguration({"/spring-test/repo.xml"})
public class ObjectServiceIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;

    @Test
    public void testGetAllObjectsDatastreamSize() throws Exception {
        Session session = repository.login();

        final double originalSize = objectService.getRepositorySize();

        datastreamService.createDatastreamNode(session,
                "testObjectServiceNode", "application/octet-stream",
                new ByteArrayInputStream("asdf".getBytes()));
        session.save();
        session.logout();

        session = repository.login();

        final double afterSize = objectService.getRepositorySize();

        assertEquals(4.0, afterSize - originalSize);

        session.logout();
    }

    @Test
    public void testGetNamespaceRegistryGraph() throws Exception {
        final Session session = repository.login();

        final Dataset registryGraph =
            objectService.getNamespaceRegistryDataset(session);

        final NamespaceRegistry namespaceRegistry =
            session.getWorkspace().getNamespaceRegistry();

        logger.info(namespaceRegistry.toString());
        logger.info(registryGraph.toString());
        for (final String s : namespaceRegistry.getPrefixes()) {
            if (s.isEmpty() || s.equals("xmlns") || s.equals("jcr")) {
                continue;
            }
            final String uri = namespaceRegistry.getURI(s);
            assertTrue("expected to find JCR namespaces " + s + " in graph",
                    registryGraph.asDatasetGraph().contains(Node.ANY,
                            ResourceFactory.createResource(uri).asNode(),
                            RdfLexicon.HAS_NAMESPACE_PREFIX.asNode(),
                            ResourceFactory.createPlainLiteral(s).asNode()));
        }
        session.logout();
    }

    @Test
    public void testUpdateNamespaceRegistryGraph() throws Exception {
        final Session session = repository.login();

        final Dataset registryGraph =
            objectService.getNamespaceRegistryDataset(session);
        final NamespaceRegistry namespaceRegistry =
            session.getWorkspace().getNamespaceRegistry();

        UpdateAction.parseExecute("INSERT { <info:abc> <"
                + HAS_NAMESPACE_PREFIX.toString() + "> \"abc\" } WHERE { }",
                registryGraph);

        assertEquals("abc", namespaceRegistry.getPrefix("info:abc"));
        session.logout();
    }

    @Test
    public void testBackupRepository() throws Exception {
        final Session session = repository.login();

        datastreamService.createDatastreamNode(session,
                "testObjectServiceNode0", "application/octet-stream",
                new ByteArrayInputStream("asdfx".getBytes()));
        session.save();

        final File backupDirectory = createTempDir();

        final Problems problems =
            objectService.backupRepository(session, backupDirectory);

        assertFalse(problems.hasProblems());
        session.logout();
    }

    @Test
    public void testRestoreRepository() throws Exception {
        final Session session = repository.login();

        datastreamService.createDatastreamNode(session,
                "testObjectServiceNode1", "application/octet-stream",
                new ByteArrayInputStream("asdfy".getBytes()));
        session.save();

        final File backupDirectory = createTempDir();

        objectService.backupRepository(session, backupDirectory);

        final Problems problems =
            objectService.restoreRepository(session, backupDirectory);

        assertFalse(problems.hasProblems());
        session.logout();
    }
}
