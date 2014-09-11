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
package org.fcrepo.integration.connector.file;

import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.junit.Test;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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

    @Test
    public void testWriteProperty() throws RepositoryException {
        final Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, testFilePath());
        assertNotNull(object);

        final String sparql = "PREFIX fedora: <http://fedora.info/definitions/v4/rest-api#> " +
                "INSERT DATA { " +
                "<info:fedora" + testFilePath() + "> " +
                "fedora:name " +
                "'some-test-name' }";


        // Write the properties
        object.updatePropertiesDataset(new DefaultIdentifierTranslator(), sparql);

        // Verify
        final Property property = object.getNode().getProperty("fedora:name");
        assertNotNull(property);
        assertEquals("some-test-name", property.getValues()[0].toString());

        session.save();
        session.logout();
    }

    @Test
    public void testRemoveProperty() throws RepositoryException {
        final Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, testFilePath());
        assertNotNull(object);

        final String sparql = "PREFIX fedora: <http://fedora.info/definitions/v4/rest-api#> " +
                "INSERT DATA { " +
                "<info:fedora" + testFilePath() + "> " +
                "fedora:remove " +
                "'some-property-to-remove' }";

        // Write the properties
        final IdentifierTranslator graphSubjects = new DefaultIdentifierTranslator();
        object.updatePropertiesDataset(graphSubjects, sparql);

        // Verify property exists
        final Property property = object.getNode().getProperty("fedora:remove");
        assertNotNull(property);
        assertEquals("some-property-to-remove", property.getValues()[0].getString());

        final String sparqlRemove = "PREFIX fedora: <http://fedora.info/definitions/v4/rest-api#> " +
                "DELETE {" +
                "  <info:fedora" + testFilePath() + "> fedora:remove ?s " +
                "} WHERE { " +
                "  <info:fedora" + testFilePath() + "> fedora:remove ?s" +
                "}";

        // Remove the properties
        final IdentifierTranslator graphSubjectsRemove = new DefaultIdentifierTranslator();
        object.updatePropertiesDataset(graphSubjectsRemove, sparqlRemove);

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

        session.logout();
    }
}
