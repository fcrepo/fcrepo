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
package org.fcrepo.kernel.modeshape.rdf.impl;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.AccessControlException;

import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDboolean;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/1/14
 */
public class AclRdfContextTest {

    @Mock
    private FedoraResource resource;

    @Mock
    private Node mockNode;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    @Mock
    private Session mockSession;

    private Resource nodeSubject;
    private String path = "/path/to/node";

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        // read-only resource mocks
        when(resource.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(resource.getPath()).thenReturn(path);
        idTranslator = new DefaultIdentifierTranslator(mockSession);
        nodeSubject = idTranslator.reverse().convert(resource);
    }

    @Test
    public void testWritableNode() throws RepositoryException {
        final Model actual = new AclRdfContext(resource, idTranslator).asModel();
        final Literal booleanTrue = actual.createTypedLiteral("true", XSDboolean);
        assertTrue("Didn't find writable triple!", actual.contains(nodeSubject, WRITABLE, booleanTrue));
    }

    @Test
    public void testReadOnlyNode() throws RepositoryException, IOException {

        doThrow(new AccessControlException("permissions check failed")).when(mockSession).checkPermission(
                eq(path), eq("add_node,set_property,remove"));
        final Model actual = new AclRdfContext(resource, idTranslator).asModel();
        logRdf("Constructed RDF: ", actual);
        final Literal booleanFalse = actual.createTypedLiteral(false, XSDboolean);
        assertTrue("Didn't find writable triple!", actual.contains(nodeSubject, WRITABLE, booleanFalse));
    }

    private static void logRdf(final String message, final Model model) throws IOException {
        LOGGER.debug(message);
        try (Writer w = new StringWriter()) {
            model.write(w);
            LOGGER.debug("\n" + w.toString());
        }
    }

    private static final Logger LOGGER = getLogger(AclRdfContextTest.class);


}
