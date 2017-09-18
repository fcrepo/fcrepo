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
package org.fcrepo.auth.webac;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.stream.Stream.of;
import static org.apache.jena.riot.Lang.TTL;
import static org.fcrepo.auth.webac.URIConstants.FOAF_GROUP;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESS_CONTROL_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AUTHORIZATION;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE_VALUE;
import static org.fcrepo.auth.webac.WebACRolesProvider.ROOT_AUTHORIZATION_PROPERTY;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URI;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Session;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.auth.roles.common.AccessRolesProvider;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author acoburn
 * @since 9/3/15
 */
@RunWith(MockitoJUnitRunner.class)
public class WebACRolesProviderTest {

    private AccessRolesProvider roleProvider;

    private static final String FEDORA_PREFIX = "info:fedora";
    private static final String FEDORA_URI_PREFIX = "file:///rest";

    @Mock
    private Node mockNode, mockParentNode;

    @Mock
    private FedoraSessionImpl mockSession;

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private Session mockJcrSession;

    @Mock
    private NodeService mockNodeService;

    @Mock
    private FedoraResourceImpl mockResource, mockParentResource;

    @Mock
    private FedoraResource mockAclResource, mockAgentClassResource,
            mockAuthorizationResource1, mockAuthorizationResource2;

    @Mock
    private Property mockProperty;

    @Before
    public void setUp() throws RepositoryException {

        roleProvider = new WebACRolesProvider();
        setField(roleProvider, "nodeService", mockNodeService);
        setField(roleProvider, "sessionFactory", mockSessionFactory);

        when(mockNodeService.find(any(FedoraSession.class), any())).thenReturn(mockResource);
        when(mockNode.getSession()).thenReturn(mockJcrSession);
        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockSession.getJcrSession()).thenReturn(mockJcrSession);

        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNode.getDepth()).thenReturn(0);
    }

    @Test
    public void noAclTest() throws RepositoryException {
        final String accessTo = "/dark/archive/sunshine";

        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getContainer()).thenReturn(mockParentResource);
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(new DefaultRdfStream(createURI("subject")));
        when(mockNode.getDepth()).thenReturn(1);

        when(mockParentResource.getNode()).thenReturn(mockParentNode);
        when(mockParentResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(new DefaultRdfStream(createURI("subject")));
        when(mockParentNode.getDepth()).thenReturn(0);

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertTrue("There should be no agents in the roles map", roles.isEmpty());
    }

    @Test
    public void acl01ParentTest() throws RepositoryException {
        final String agent = "user01";
        final String accessTo = "/webacl_box1";
        final String acl = "/acls/01";
        final String auth = acl + "/authorization.ttl";

        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getContainer()).thenReturn(mockParentResource);
        when(mockResource.getPath()).thenReturn(accessTo + "/foo");
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(new DefaultRdfStream(createURI("subject")));
        when(mockNode.getDepth()).thenReturn(1);

        when(mockParentResource.getNode()).thenReturn(mockParentNode);
        when(mockParentResource.getPath()).thenReturn(accessTo);
        when(mockParentResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));
        when(mockParentNode.getDepth()).thenReturn(0);

        when(mockProperty.getString()).thenReturn(acl);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent in the role map", 1, roles.size());
        assertEquals("The agent should have exactly two modes", 2, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
        assertTrue("The agent should be able to write", roles.get(agent).contains(WEBAC_MODE_WRITE_VALUE));
    }

    @Test
    public void acl01Test1() throws RepositoryException {
        final String agent = "user01";
        final String accessTo = "/webacl_box1";
        final String acl = "/acls/01";
        final String auth = acl + "/authorization.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent in the role map", 1, roles.size());
        assertEquals("The agent should have exactly two modes", 2, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
        assertTrue("The agent should be able to write", roles.get(agent).contains(WEBAC_MODE_WRITE_VALUE));
    }

    @Test
    public void acl01Test2() throws RepositoryException {
        final String accessTo = "/webacl_box2";
        final String acl = "/acls/01";
        final String auth = acl + "/authorization.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertTrue("There should be no agents associated with this object", roles.isEmpty());
    }

    @Test
    public void acl02Test() throws RepositoryException {
        final String agent = "Editors";
        final String accessTo = "/box/bag/collection";
        final String acl = "/acls/02";
        final String auth = acl + "/authorization.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent in the role map", 1, roles.size());
        assertEquals("The agent should have exactly two modes", 2, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
        assertTrue("The agent should be able to write", roles.get(agent).contains(WEBAC_MODE_WRITE_VALUE));
    }

    @Test
    public void acl03Test1() throws RepositoryException {
        final String agent = "http://xmlns.com/foaf/0.1/Agent";
        final String accessTo = "/dark/archive/sunshine";
        final String acl = "/acls/03";
        final String auth1 = acl + "/auth_restricted.ttl";
        final String auth2 = acl + "/auth_open.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth1);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth1, TTL));

        when(mockAuthorizationResource2.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource2.getPath()).thenReturn(auth2);
        when(mockAuthorizationResource2.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth2, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1, mockAuthorizationResource2));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent in the roles map", 1, roles.size());
        assertEquals("The agent should have exactly one mode", 1, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void acl03Test2() throws RepositoryException {
        final String agent = "Restricted";
        final String accessTo = "/dark/archive";
        final String acl = "/acls/03";
        final String auth1 = acl + "/auth_restricted.ttl";
        final String auth2 = acl + "/auth_open.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth1);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth1, TTL));

        when(mockAuthorizationResource2.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource2.getPath()).thenReturn(auth2);
        when(mockAuthorizationResource2.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth2, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1, mockAuthorizationResource2));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent", 1, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void acl04Test() throws RepositoryException {
        final String agent1 = "http://xmlns.com/foaf/0.1/Agent";
        final String agent2 = "Editors";
        final String accessTo = "/public_collection";
        final String acl = "/acls/04";
        final String auth1 = acl + "/auth1.ttl";
        final String auth2 = acl + "/auth2.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth1);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth1, TTL));

        when(mockAuthorizationResource2.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource2.getPath()).thenReturn(auth2);
        when(mockAuthorizationResource2.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth2, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1, mockAuthorizationResource2));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly two agents", 2, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent1).size());
        assertTrue("The agent should be able to read", roles.get(agent1).contains(WEBAC_MODE_READ_VALUE));
        assertEquals("The agent should have two modes", 2, roles.get(agent2).size());
        assertTrue("The agent should be able to read", roles.get(agent2).contains(WEBAC_MODE_READ_VALUE));
        assertTrue("The agent should be able to write", roles.get(agent2).contains(WEBAC_MODE_READ_VALUE));
    }

    public void acl05Test() throws RepositoryException {
        final String agent1 = "http://xmlns.com/foaf/0.1/Agent";
        final String agent2 = "Admins";
        final String accessTo = "/mixedCollection";
        final String acl = "/acls/05";
        final String auth1 = acl + "/auth_restricted.ttl";
        final String auth2 = acl + "/auth_open.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTypes()).thenReturn(Arrays.asList(URI.create("http://example.com/terms#publicImage")));
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth1);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth1, TTL));

        when(mockAuthorizationResource2.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource2.getPath()).thenReturn(auth2);
        when(mockAuthorizationResource2.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth2, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1, mockAuthorizationResource2));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly two agents", 2, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent1).size());
        assertTrue("The agent should be able to read", roles.get(agent1).contains(WEBAC_MODE_READ_VALUE));
        assertEquals("The agent should have one mode", 1, roles.get(agent2).size());
        assertTrue("The agent should be able to read", roles.get(agent2).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void acl05Test2() throws RepositoryException {
        final String agent1 = "http://xmlns.com/foaf/0.1/Agent";
        final String accessTo = "/someOtherCollection";
        final String acl = "/acls/05";
        final String auth1 = acl + "/auth_restricted.ttl";
        final String auth2 = acl + "/auth_open.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTypes()).thenReturn(Arrays.asList(URI.create("http://example.com/terms#publicImage")));
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth1);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth1, TTL));

        when(mockAuthorizationResource2.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource2.getPath()).thenReturn(auth2);
        when(mockAuthorizationResource2.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth2, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1, mockAuthorizationResource2));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly agent", 1, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent1).size());
        assertTrue("The agent should be able to read", roles.get(agent1).contains(WEBAC_MODE_READ_VALUE));
    }

    /* (non-Javadoc)
     * Test that an in-repository resource used as a target for acl:agentClass has
     * the rdf:type of foaf:Group. This test mocks a foaf:Group resource and should
     * therefore retrieve two agents.
     */
    @Test
    public void acl09Test1() throws RepositoryException {
        final String agent1 = "person1";
        final String agent2 = "person2";
        final String accessTo = "/anotherCollection";

        final String groupResource = "/group/foo";
        final String acl = "/acls/09";
        final String auth = acl + "/authorization.ttl";
        final String group = acl + "/group.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, groupResource)).thenReturn(mockAgentClassResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTypes()).thenReturn(new ArrayList<>());
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth, TTL));

        when(mockAgentClassResource.getTypes()).thenReturn(Arrays.asList(FOAF_GROUP));
        when(mockAgentClassResource.getPath()).thenReturn(groupResource);
        when(mockAgentClassResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(group, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly two agents", 2, roles.size());
        assertEquals("The agent should have two modes", 2, roles.get(agent1).size());
        assertTrue("The agent should be able to read", roles.get(agent1).contains(WEBAC_MODE_READ_VALUE));
        assertTrue("The agent should be able to write", roles.get(agent1).contains(WEBAC_MODE_WRITE_VALUE));
    }

    /* (non-Javadoc)
     * Test that an in-repository resource used as a target for acl:agentClass has
     * the rdf:type of foaf:Group. This test mocks a resource that is not of the type
     * foaf:Group and therefore should retrieve zero agents.
     */
    @Test
    public void acl09Test2() throws RepositoryException {
        final String agent1 = "person1";
        final String agent2 = "person2";
        final String accessTo = "/anotherCollection";

        final String groupResource = "/group/foo";
        final String acl = "/acls/09";
        final String auth = acl + "/authorization.ttl";
        final String group = acl + "/group.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, groupResource)).thenReturn(mockAgentClassResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getTypes()).thenReturn(new ArrayList<>());
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getResourceRdfStream(accessTo, acl));

        when(mockAuthorizationResource1.getTypes()).thenReturn(Arrays.asList(WEBAC_AUTHORIZATION));
        when(mockAuthorizationResource1.getPath()).thenReturn(auth);
        when(mockAuthorizationResource1.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(auth, TTL));

        when(mockAgentClassResource.getTypes()).thenReturn(new ArrayList<>());
        when(mockAgentClassResource.getPath()).thenReturn(groupResource);
        when(mockAgentClassResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(group, TTL));

        when(mockAclResource.getChildren()).thenReturn(of(mockAuthorizationResource1));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly zero agents", 0, roles.size());
    }

    @Test
    public void noAclTest1() throws RepositoryException {
        final String agent1 = "http://xmlns.com/foaf/0.1/Agent";

        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(new DefaultRdfStream(createURI("subject")));
        when(mockResource.getPath()).thenReturn("/");
        when(mockResource.getTypes()).thenReturn(
                Arrays.asList(URI.create(REPOSITORY_NAMESPACE + "Resource")));
        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent", 1, roles.size());
        assertEquals("The agent should have zero modes", 0, roles.get(agent1).size());
    }

    @Test
    public void noAclTestMalformedRdf2() throws RepositoryException {
        final String agent1 = "http://xmlns.com/foaf/0.1/Agent";

        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(new DefaultRdfStream(createURI("subject")));
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(new DefaultRdfStream(createURI("subject")));
        when(mockResource.getPath()).thenReturn("/");
        when(mockResource.getTypes()).thenReturn(
                Arrays.asList(URI.create(REPOSITORY_NAMESPACE + "Resource")));

        System.setProperty(ROOT_AUTHORIZATION_PROPERTY, "./target/test-classes/logback-test.xml");
        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);
        System.clearProperty(ROOT_AUTHORIZATION_PROPERTY);

        assertEquals("There should be exactly one agent", 1, roles.size());
        assertEquals("The agent should have zero modes", 0, roles.get(agent1).size());
    }

    @Test
    public void noAclTestOkRdf3() throws RepositoryException {
        final String agent1 = "testAdminUser";

        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(new DefaultRdfStream(createURI("subject")));
        when(mockResource.getPath()).thenReturn("/");
        when(mockResource.getTypes()).thenReturn(
                Arrays.asList(URI.create(REPOSITORY_NAMESPACE + "Resource")));

        System.setProperty(ROOT_AUTHORIZATION_PROPERTY, "./target/test-classes/test-root-authorization.ttl");
        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);
        System.clearProperty(ROOT_AUTHORIZATION_PROPERTY);

        assertEquals("There should be exactly one agent", 1, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent1).size());
        assertTrue("The agent should be able to read", roles.get(agent1).contains(WEBAC_MODE_READ_VALUE));
    }

    private static RdfStream getRdfStreamFromResource(final String resourcePath, final Lang lang) {
        final Model model = createDefaultModel();

        RDFDataMgr.read(model, WebACRolesProviderTest.class.getResourceAsStream(resourcePath), lang);

        final List<Triple> triples = new ArrayList<>();
        model.listStatements().forEachRemaining(x -> {
            final Triple t = x.asTriple();
            if (t.getObject().isURI() && t.getObject().getURI().startsWith(FEDORA_URI_PREFIX)) {
                triples.add(new Triple(t.getSubject(), t.getPredicate(),
                        createURI(FEDORA_PREFIX + t.getObject().getURI().substring(FEDORA_URI_PREFIX.length()))));
            } else {
                triples.add(t);
            }
        });

        return new DefaultRdfStream(createURI("subject"), triples.stream());
    }

    private RdfStream getResourceRdfStream(final String subject, final String aclTarget) {
        return new DefaultRdfStream(createURI(FEDORA_PREFIX + subject), of(
                    create(createURI(FEDORA_PREFIX + subject),
                               createURI(WEBAC_ACCESS_CONTROL_VALUE),
                               createURI(FEDORA_PREFIX + aclTarget))
                ));
    }
}
