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
package org.fcrepo.kernel.modeshape.rdf.impl;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.security.AccessControlException;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDboolean;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author cabeer
 * @since 10/1/14
 */
@RunWith(MockitoJUnitRunner.class)
public class AclRdfContextTest {

    @Mock
    private FedoraResourceImpl resource;

    @Mock
    private Node mockNode;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    @Mock
    private Session mockSession;

    private Resource nodeSubject;
    private final String path = "/path/to/node";

    @Before
    public void setUp() throws RepositoryException {
        // read-only resource mocks
        when(resource.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(resource.getPath()).thenReturn(path);
        idTranslator = new DefaultIdentifierTranslator(mockSession);
        nodeSubject = idTranslator.reverse().convert(resource);
    }

    @Test
    public void testWritableNode() throws RepositoryException {
        try (final AclRdfContext aclRdfContext = new AclRdfContext(resource, idTranslator)) {
            final Model actual = aclRdfContext.collect(toModel());
            final Literal booleanTrue = actual.createTypedLiteral("true", XSDboolean);
            assertTrue("Didn't find writable triple!", actual.contains(nodeSubject, WRITABLE, booleanTrue));
        }
    }

    @Test
    public void testReadOnlyNode() throws RepositoryException {

        doThrow(new AccessControlException("permissions check failed")).when(mockSession).checkPermission(
                eq(path), eq("add_node,set_property,remove"));
        try (final AclRdfContext aclRdfContext = new AclRdfContext(resource, idTranslator)) {
            final Model actual = aclRdfContext.collect(toModel());
            final Literal booleanFalse = actual.createTypedLiteral(false, XSDboolean);
            assertTrue("Didn't find writable triple!", actual.contains(nodeSubject, WRITABLE, booleanFalse));
        }
    }
}
