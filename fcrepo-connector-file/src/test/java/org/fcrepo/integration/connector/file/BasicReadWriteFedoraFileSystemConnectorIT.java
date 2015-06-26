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

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.junit.Test;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Durbin
 */
public class BasicReadWriteFedoraFileSystemConnectorIT extends AbstractFedoraFileSystemConnectorIT {

    @Override
    protected String federationName() {
        return "federated";
    }

    @Override
    protected String testFilePath() {
        return "/" + federationName() + "/repository.json";
    }

    @Override
    protected String testDirPath() {
        return "/" + federationName();
    }

    @Override
    protected String getFederationRoot() {
        return getReadWriteFederationRoot();
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testWriteProperty() throws RepositoryException {
        final Session session = repo.login();
        try {
            final FedoraResource object = nodeService.find(session, testFilePath());
            assertNotNull(object);

            final String sparql = "PREFIX fedora: <" + REPOSITORY_NAMESPACE + "> " +
                    "INSERT DATA { " +
                    "<info:fedora" + testFilePath() + "> " +
                    "fedora:name " +
                    "'some-test-name' }";

            // Write the properties
            object.updateProperties(new DefaultIdentifierTranslator(session), sparql, new RdfStream());

            // Verify
            final Property property = object.getNode().getProperty("fedora:name");
            assertNotNull(property);
            assertEquals("some-test-name", property.getValues()[0].toString());

            session.save();
        } finally {
            session.logout();
        }
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testRemoveProperty() throws RepositoryException {
        final Session session = repo.login();
        try {
            final FedoraResource object = nodeService.find(session, testFilePath());
            assertNotNull(object);

            final String sparql = "PREFIX fedora: <" + REPOSITORY_NAMESPACE + "> " +
                    "INSERT DATA { " +
                    "<info:fedora" + testFilePath() + "> " +
                    "fedora:remove " +
                    "'some-property-to-remove' }";

            // Write the properties
            final DefaultIdentifierTranslator graphSubjects = new DefaultIdentifierTranslator(session);
            object.updateProperties(graphSubjects, sparql, new RdfStream());

            // Verify property exists
            final Property property = object.getNode().getProperty("fedora:remove");
            assertNotNull(property);
            assertEquals("some-property-to-remove", property.getValues()[0].getString());

            final String sparqlRemove = "PREFIX fedora: <" + REPOSITORY_NAMESPACE + "> " +
                    "DELETE {" +
                    "  <info:fedora" + testFilePath() + "> fedora:remove ?s " +
                    "} WHERE { " +
                    "  <info:fedora" + testFilePath() + "> fedora:remove ?s" +
                    "}";

            // Remove the properties
            object.updateProperties(graphSubjects,
                    sparqlRemove,
                    object.getTriples(graphSubjects, PropertiesRdfContext.class));

            // Persist the object (although the propery will be removed from memory without this.)
            session.save();

            // Verify
            boolean thrown = false;
            try {
                object.getNode().getProperty("fedora:remove");
            } catch (final PathNotFoundException e) {
                thrown = true;
            }
            assertTrue("Exception expected - property should be missing", thrown);
        } finally {
            session.logout();
        }
    }
}
