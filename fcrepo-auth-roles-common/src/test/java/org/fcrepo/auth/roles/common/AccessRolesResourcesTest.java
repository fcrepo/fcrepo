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
package org.fcrepo.auth.roles.common;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.RdfLexicon;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author bbpennel
 * @since Feb 17, 2014
 */
public class AccessRolesResourcesTest {

    @Mock
    private HttpIdentifierTranslator graphSubjects;

    @Mock
    private FedoraResource fedoraResource;

    private Resource graphResource;

    @Mock
    private Node resourceNode;

    private Model model;

    private UriInfo uriInfo;

    private AccessRolesResources resources;

    private String pathString;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        resources = new AccessRolesResources();

        pathString = "path";
        model = ModelFactory.createDefaultModel();
        graphResource = model.createResource("/" + pathString);

        when(graphSubjects.getSubject(Matchers.anyString())).thenReturn(
                graphResource);
        when(fedoraResource.getNode()).thenReturn(resourceNode);

        uriInfo = getUriInfoImpl();
    }

    @Test
    public void testCreateModelForNonFedoraResource()
            throws RepositoryException {

        when(resourceNode.isNodeType(eq(FedoraJcrTypes.FEDORA_RESOURCE)))
                .thenReturn(false);

        final Model model =
                resources.createModelForResource(fedoraResource, uriInfo,
                        graphSubjects);

        assertTrue("Model should be an empty default model", model.isEmpty());
    }

    @Test
    public void testCreateModelForResource() throws RepositoryException {
        when(resourceNode.isNodeType(eq(FedoraJcrTypes.FEDORA_RESOURCE)))
                .thenReturn(true);

        when(resourceNode.getPath()).thenReturn("/" + pathString);
        when(fedoraResource.getPath(graphSubjects)).thenReturn("/" + pathString);
        final Model model =
                resources.createModelForResource(fedoraResource, uriInfo,
                        graphSubjects);

        assertFalse("Model should not be empty", model.isEmpty());

        final ResIterator resIterator =
                model.listResourcesWithProperty(RdfLexicon.HAS_ACCESS_ROLES_SERVICE);

        assertTrue(
                "No resources with property HAS_ACCESS_ROLES_SERVICE in model",
                resIterator.hasNext());

        final Resource addedResource = resIterator.next();

        assertEquals(
                "Resource localname should match URI of provided resource",
                pathString, addedResource.getLocalName());
    }
}
