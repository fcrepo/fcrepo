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

import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.Lang.TTL;
import static org.fcrepo.auth.webac.URIConstants.VCARD_GROUP;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESS_CONTROL_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE_VALUE;
import static org.fcrepo.http.api.FedoraAcl.ROOT_AUTHORIZATION_PROPERTY;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author acoburn
 * @since 9/3/15
 */
@RunWith(MockitoJUnitRunner.class)
public class WebACRolesProviderTest {

    private WebACRolesProvider roleProvider;

    private static final String FEDORA_PREFIX = "info:fedora";
    private static final String FEDORA_URI_PREFIX = "file:///rest";

    @Mock
    private Node mockNode, mockAclNode, mockParentNode;

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
    private FedoraResourceImpl mockAclResource;

    @Mock
    private FedoraResource mockAgentClassResource,
            mockAuthorizationResource1, mockAuthorizationResource2;

    @Mock
    private Property mockProperty;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Before
    public void setUp() throws RepositoryException {

        roleProvider = new WebACRolesProvider();
        setField(roleProvider, "nodeService", mockNodeService);
        setField(roleProvider, "sessionFactory", mockSessionFactory);

        when(mockNode.getSession()).thenReturn(mockJcrSession);
        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockSession.getJcrSession()).thenReturn(mockJcrSession);

        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockAclResource.getNode()).thenReturn(mockAclNode);

        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockNode.getDepth()).thenReturn(0);
    }

    @Test
    public void noAclTest() throws RepositoryException {
        final String accessTo = "/dark/archive/sunshine";

        when(mockResource.getAcl()).thenReturn(null);
        when(mockParentResource.getAcl()).thenReturn(null);

        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);

        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getContainer()).thenReturn(mockParentResource);
        when(mockResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(new DefaultRdfStream(createURI("subject")));
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockNode.getDepth()).thenReturn(1);

        when(mockParentResource.getNode()).thenReturn(mockParentNode);
        when(mockParentResource.getOriginalResource()).thenReturn(mockParentResource);
        when(mockParentNode.getDepth()).thenReturn(0);

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertTrue("There should be no agents in the roles map", roles.isEmpty());
    }

    @Test
    public void acl01ParentTest() throws RepositoryException {
        final String agent = "user01";
        final String parentPath = "/webacl_box1";
        final String accessTo = parentPath + "/foo";
        final String acl = "/acls/01/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);

        when(mockResource.getAcl()).thenReturn(null);
        when(mockParentResource.getAcl()).thenReturn(mockAclResource);

        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getContainer()).thenReturn(mockParentResource);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockNode.getDepth()).thenReturn(1);

        when(mockParentResource.getNode()).thenReturn(mockParentNode);
        when(mockParentResource.getPath()).thenReturn(parentPath);
        when(mockParentResource.getAcl()).thenReturn(mockAclResource);
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockAclResource.getPath()).thenReturn(parentPath + "/fcr:acl");

        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(acl, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent in the role map", 1, roles.size());
        assertEquals("The agent should have exactly two modes", 2, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
        assertTrue("The agent should be able to write", roles.get(agent).contains(WEBAC_MODE_WRITE_VALUE));
    }

    @Test
    public void acl21NoDefaultACLStatementTest() throws RepositoryException {
        final String agent = "user21";
        final String parentPath = "/resource_acl_no_inheritance";
        final String accessTo = parentPath + "/foo";
        final String acl = "/acls/21/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);

        when(mockResource.getAcl()).thenReturn(null);
        when(mockParentResource.getAcl()).thenReturn(mockAclResource);
        when(mockAclResource.hasProperty("acl:default")).thenReturn(false);

        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getContainer()).thenReturn(mockParentResource);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockNode.getDepth()).thenReturn(1);

        when(mockParentResource.getNode()).thenReturn(mockParentNode);
        when(mockParentResource.getPath()).thenReturn(parentPath);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockParentNode.getDepth()).thenReturn(0);


        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(acl, TTL));

        System.setProperty(ROOT_AUTHORIZATION_PROPERTY, "./target/test-classes/test-root-authorization2.ttl");

        // The default root ACL should be used for authorization instead of the parent ACL
        final String rootAgent = "user06a";
        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);
        assertEquals("Contains no agents in the role map!", 1, roles.size());
        assertNull("Contains agent " + agent + " from ACL in parent node!", roles.get(agent));
        assertEquals("Should have agent " + rootAgent + " from the root ACL!", 1, roles.get(rootAgent).size());
        assertTrue("Should have read mode for agent " + rootAgent + " from the root ACL!",
                roles.get(rootAgent).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void acl01Test1() throws RepositoryException {
        final String agent = "user01";
        final String accessTo = "/webacl_box1";
        final String acl = "/acls/01/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockAclResource.getPath()).thenReturn(accessTo + "/fcr:acl");


        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent in the role map", 1, roles.size());
        assertEquals("The agent should have exactly two modes", 2, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
        assertTrue("The agent should be able to write", roles.get(agent).contains(WEBAC_MODE_WRITE_VALUE));
    }

    @Test
    public void acl01Test2() throws RepositoryException {
        final String accessTo = "/webacl_box2";
        final String acl = "/acls/01/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo ))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertTrue("There should be no agents associated with this object", roles.isEmpty());
    }

    @Test
    public void acl02Test() throws RepositoryException {
        final String agent = "Editors";
        final String accessTo = "/box/bag/collection";
        final String acl = "/acls/02/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);

        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockProperty.getString()).thenReturn(acl);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockAclResource.getPath()).thenReturn(accessTo + "/fcr:acl");

        when(mockResource.getOriginalResource()).thenReturn(mockResource);


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
        final String acl = "/acls/03/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo ))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockAclResource.getPath()).thenReturn(accessTo + "/fcr:acl");

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent in the roles map", 1, roles.size());
        assertEquals("The agent should have exactly one mode", 1, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void acl03Test2() throws RepositoryException {
        final String agent = "Restricted";
        final String accessTo = "/dark/archive";
        final String acl = "/acls/03/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo ))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent", 1, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent).size());
        assertTrue("The agent should be able to read", roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void foafAgentTest() throws RepositoryException {
        final String agent = "http://xmlns.com/foaf/0.1/Agent";
        final String accessTo = "/foaf-agent";
        final String acl = "/acls/03/foaf-agent.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be only one valid role", 1, roles.size());
        assertEquals("The foaf:Agent should have exactly one valid mode", 1,
                     roles.get(agent).size());
        assertTrue("The foaf:Agent should be able to write",
                   roles.get(agent).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void authenticatedAgentTest() throws RepositoryException {
        final String aclAuthenticatedAgent = "http://www.w3.org/ns/auth/acl#AuthenticatedAgent";
        final String accessTo = "/authenticated-agent";
        final String acl = "/acls/03/authenticated-agent.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be only one valid role", 1, roles.size());
        assertEquals("The acl:AuthenticatedAgent should have exactly one valid mode", 1,
                     roles.get(aclAuthenticatedAgent).size());
        assertTrue("The acl:AuthenticatedAgent should be able to write",
                   roles.get(aclAuthenticatedAgent).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void acl04Test() throws RepositoryException {
        final String agent1 = "http://xmlns.com/foaf/0.1/Agent";
        final String agent2 = "Editors";
        final String accessTo = "/public_collection";
        final String acl = "/acls/04/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly two agents", 2, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent1).size());
        assertTrue("The agent should be able to read", roles.get(agent1).contains(WEBAC_MODE_READ_VALUE));
        assertEquals("The agent should have two modes", 2, roles.get(agent2).size());
        assertTrue("The agent should be able to read", roles.get(agent2).contains(WEBAC_MODE_READ_VALUE));
        assertTrue("The agent should be able to write", roles.get(agent2).contains(WEBAC_MODE_READ_VALUE));
    }

    @Test
    public void acl05Test() throws RepositoryException {
        final String agent1 = "http://xmlns.com/foaf/0.1/Agent";
        final String agent2 = "Admins";
        final String accessTo = "/mixedCollection";
        final String acl = "/acls/05/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockResource.getTypes()).thenReturn(Arrays.asList(URI.create("http://example.com/terms#publicImage")));
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

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
        final String acl = "/acls/05/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockResource.getTypes()).thenReturn(Arrays.asList(URI.create("http://example.com/terms#publicImage")));
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent", 1, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent1).size());
        assertTrue("The agent should be able to read", roles.get(agent1).contains(WEBAC_MODE_READ_VALUE));
    }

    /* (non-Javadoc)
     * Test that an in-repository resource used as a target for acl:agentGroup has
     * the rdf:type of vcard:Group. This test mocks a vcard:Group resource and should
     * therefore retrieve two agents.
     */
    @Test
    public void acl09Test1() throws RepositoryException {
        final String agent1 = "person1";
        final String agent2 = "person2";
        final String accessTo = "/anotherCollection";

        final String groupResource = "/group/foo";
        final String aclDir = "/acls/09";
        final String acl = aclDir + "/acl.ttl";
        final String group = aclDir + "/group.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, groupResource)).thenReturn(mockAgentClassResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockAclResource.getPath()).thenReturn(accessTo + "/fcr:acl");

        when(mockAgentClassResource.getTypes()).thenReturn(Arrays.asList(VCARD_GROUP));
        when(mockAgentClassResource.getPath()).thenReturn(groupResource);
        when(mockAgentClassResource.getTriples(anyObject(), eq(PROPERTIES)))
                .thenReturn(getRdfStreamFromResource(group, TTL));


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
        final String acl = "/acls/09/acl.ttl";
        final String group = "/acls/09/group.ttl";

        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, groupResource)).thenReturn(mockAgentClassResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

        when(mockAgentClassResource.getTypes()).thenReturn(new ArrayList<>());
        when(mockAgentClassResource.getPath()).thenReturn(groupResource);
        when(mockAgentClassResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(group, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly zero agents", 0, roles.size());
    }

    @Test
    public void acl17Test1() throws RepositoryException {
        final String foafAgent = "http://xmlns.com/foaf/0.1/Agent";
        final String accessTo = "/dark/archive/sunshine";
        final String acl = "/acls/17/acl.ttl";

        when(mockNodeService.find(any(FedoraSession.class), eq(acl))).thenReturn(mockAclResource);
        when(mockNodeService.find(any(FedoraSession.class), eq(accessTo))).thenReturn(mockResource);
        when(mockNode.getPath()).thenReturn(accessTo);
        when(mockAclNode.getPath()).thenReturn(acl);
        when(mockResource.getAcl()).thenReturn(mockAclResource);
        when(mockNodeService.find(mockSession, acl)).thenReturn(mockAclResource);
        when(mockAclResource.getPath()).thenReturn(acl);
        when(mockAclResource.isAcl()).thenReturn(true);
        when(mockResource.getPath()).thenReturn(accessTo);
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        when(mockAclResource.getTriples(anyObject(), eq(PROPERTIES)))
            .thenReturn(getRdfStreamFromResource(acl, TTL));

        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be only one valid role", 1, roles.size());
        assertEquals("The foafAgent should have exactly one valid mode", 1, roles.get(foafAgent).size());
        assertTrue("The foafAgent should be able to write", roles.get(foafAgent).contains(WEBAC_MODE_WRITE_VALUE));
    }

    @Test
    public void noAclTest1() throws RepositoryException {
        final String agent1 = "http://xmlns.com/foaf/0.1/Agent";

        when(mockNodeService.find(any(FedoraSession.class), any())).thenReturn(mockResource);
        when(mockResource.getAcl()).thenReturn(null);

        when(mockResource.getPath()).thenReturn("/");
        when(mockResource.getTypes()).thenReturn(
                Arrays.asList(URI.create(REPOSITORY_NAMESPACE + "Resource")));
        when(mockResource.getOriginalResource()).thenReturn(mockResource);
        final Map<String, Collection<String>> roles = roleProvider.getRoles(mockNode, true);

        assertEquals("There should be exactly one agent", 1, roles.size());
        assertEquals("The agent should have one mode", 1, roles.get(agent1).size());
    }

    @Test(expected = RuntimeException.class)
    public void noAclTestMalformedRdf2() throws RepositoryException {

        when(mockNodeService.find(any(FedoraSession.class), any())).thenReturn(mockResource);
        when(mockResource.getAcl()).thenReturn(null);

        when(mockResource.getPath()).thenReturn("/");
        when(mockResource.getTypes()).thenReturn(
                Arrays.asList(URI.create(REPOSITORY_NAMESPACE + "Resource")));
        when(mockResource.getOriginalResource()).thenReturn(mockResource);

        System.setProperty(ROOT_AUTHORIZATION_PROPERTY, "./target/test-classes/logback-test.xml");
        roleProvider.getRoles(mockNode, true);
    }

    @Test
    public void noAclTestOkRdf3() throws RepositoryException {
        final String agent1 = "testAdminUser";

        when(mockNodeService.find(any(FedoraSession.class), any())).thenReturn(mockResource);
        when(mockResource.getAcl()).thenReturn(null);
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
