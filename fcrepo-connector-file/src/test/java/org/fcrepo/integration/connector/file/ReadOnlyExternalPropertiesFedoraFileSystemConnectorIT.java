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
package org.fcrepo.integration.connector.file;

import org.fcrepo.kernel.api.models.FedoraResource;

import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static com.google.common.collect.Iterators.size;
import static org.junit.Assert.assertEquals;

/**
 * @author Mike Durbin
 */

public class ReadOnlyExternalPropertiesFedoraFileSystemConnectorIT extends AbstractFedoraFileSystemConnectorIT {

    @Override
    protected String federationName() {
        return "readonly-federated";
    }

    @Override
    protected String getFederationRoot() {
        return getReadOnlyFederationRoot();
    }

    @Override
    protected String testDirPath() {
        return "/" + federationName();
    }

    @Override
    protected String testFilePath() {
        return "/" + federationName() + "/repo.xml";
    }

    @Test
    public void verifyThatPropertiesAreExternal() throws RepositoryException {
        final Session session = repo.login();
        try {
            final FedoraResource object = nodeService.find(session, testFilePath());
            assertEquals(
                    "There should be exactly as many visible nodes as actual files (ie, no hidden sidecar files).",
                    fileForNode().getParentFile().list().length, getChildCount(object.getNode().getParent()));
        } finally {
            session.logout();
        }
    }

    protected int getChildCount(final Node node) throws RepositoryException {
        return size(node.getNodes());
    }

}
