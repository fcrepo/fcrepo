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
package org.fcrepo.http.commons.test.util;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.net.URI.create;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.junit.Assert.assertNotNull;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.riot.Lang;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.mint.UUIDPidMinter;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.query.QueryManager;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStoreFactory;

/**
 * <p>Abstract TestHelpers class.</p>
 *
 * @author awoods
 */
public abstract class TestHelpers {

    public static String MOCK_PREFIX = "mockPrefix";

    public static String MOCK_URI_STRING = "mock.namespace.org";

    public static UriInfo getUriInfoImpl() {
        // UriInfo ui = mock(UriInfo.class,withSettings().verboseLogging());
        final UriInfo ui = mock(UriInfo.class);

        final Answer<UriBuilder> answer = new Answer<UriBuilder>() {

            @Override
            public UriBuilder answer(final InvocationOnMock invocation) {
                return fromUri("http://localhost/fcrepo");
            }
        };

        when(ui.getRequestUri()).thenReturn(
                URI.create("http://localhost/fcrepo"));
        when(ui.getBaseUri()).thenReturn(create("http://localhost/fcrepo"));
        when(ui.getBaseUriBuilder()).thenAnswer(answer);
        when(ui.getAbsolutePathBuilder()).thenAnswer(answer);

        return ui;
    }

    public static Session getQuerySessionMock() {
        final Session mock = mock(Session.class);
        final Workspace mockWS = mock(Workspace.class);
        final QueryManager mockQM = mock(QueryManager.class);
        try {
            final Query mockQ = getQueryMock();
            when(mockQM.createQuery(anyString(), anyString()))
                    .thenReturn((org.modeshape.jcr.api.query.Query) mockQ);
            when(mockWS.getQueryManager()).thenReturn(mockQM);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        when(mock.getWorkspace()).thenReturn(mockWS);
        final ValueFactory mockVF = mock(ValueFactory.class);
        try {
            when(mock.getValueFactory()).thenReturn(mockVF);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static Query getQueryMock() {
        final Query mockQ = mock(Query.class);
        final QueryResult mockResults = mock(QueryResult.class);
        final NodeIterator mockNodes = mock(NodeIterator.class);
        when(mockNodes.getSize()).thenReturn(2L);
        when(mockNodes.hasNext()).thenReturn(true, true, false);
        final Node node1 = mock(Node.class);
        final Node node2 = mock(Node.class);
        try {
            when(node1.getName()).thenReturn("node1");
            when(node2.getName()).thenReturn("node2");
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        when(mockNodes.nextNode()).thenReturn(node1, node2).thenThrow(
                IndexOutOfBoundsException.class);
        try {
            when(mockResults.getNodes()).thenReturn(mockNodes);
            when(mockQ.execute()).thenReturn(mockResults);
        } catch (final RepositoryException e) {
            e.printStackTrace();
        }
        return mockQ;
    }

    @SuppressWarnings("unchecked")
    public static Session getSessionMock() throws RepositoryException {
        final String[] mockPrefixes = {MOCK_PREFIX};
        final Session mockSession = mock(Session.class);
        final Workspace mockWorkspace = mock(Workspace.class);
        final NamespaceRegistry mockNameReg = mock(NamespaceRegistry.class);
        final NodeTypeManager mockNTM = mock(NodeTypeManager.class);
        final NodeTypeIterator mockNTI = mock(NodeTypeIterator.class);
        final NodeType mockNodeType = mock(NodeType.class);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNameReg);
        when(mockNameReg.getPrefixes()).thenReturn(mockPrefixes);
        when(mockNameReg.getURI(MOCK_PREFIX)).thenReturn(MOCK_URI_STRING);
        when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNTM);
        when(mockNodeType.getName()).thenReturn("mockName");
        when(mockNodeType.toString()).thenReturn("mockString");
        when(mockNTM.getAllNodeTypes()).thenReturn(mockNTI);
        when(mockNTI.hasNext()).thenReturn(true, false);
        when(mockNTI.nextNodeType()).thenReturn(mockNodeType).thenThrow(
                ArrayIndexOutOfBoundsException.class);
        return mockSession;
    }

    public static Collection<String>
            parseChildren(final HttpEntity entity) throws IOException {
        final String body = EntityUtils.toString(entity);
        System.err.println(body);
        final String[] names =
            body.replace("[", "").replace("]", "").trim().split(",\\s?");
        return Arrays.asList(names);
    }

    public static Session mockSession(final AbstractResource testObj) {

        final SecurityContext mockSecurityContext = mock(SecurityContext.class);
        final Principal mockPrincipal = mock(Principal.class);

        final String mockUser = "testuser";

        final Session mockSession = mock(Session.class);
        when(mockSession.getUserID()).thenReturn(mockUser);
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(mockUser);

        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getName()).thenReturn("default");

        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "pidMinter", new UUIDPidMinter());
        return mockSession;

    }

    public static Repository mockRepository() throws LoginException,
                                             RepositoryException {
        final Repository mockRepo = mock(Repository.class);
        when(mockRepo.login()).thenReturn(
                mock(org.modeshape.jcr.api.Session.class, Mockito.withSettings().extraInterfaces(Session.class)));
        return mockRepo;
    }

    public static NonRdfSourceDescription mockDatastream(final String pid,
                                            final String dsId, final String content) {
        final FedoraBinary mockBinary = mockBinary(pid, dsId, content);
        final NonRdfSourceDescription mockDs = mock(NonRdfSourceDescription.class);
        when(mockDs.getDescribedResource()).thenReturn(mockBinary);
        when(mockDs.getDescribedResource().getDescription()).thenReturn(mockDs);
        when(mockDs.getPath()).thenReturn("/" + pid + "/" + dsId);
        when(mockDs.getCreatedDate()).thenReturn(new Date());
        when(mockDs.getLastModifiedDate()).thenReturn(new Date());
        if (content != null) {
            final MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            final byte[] digest = md.digest(content.getBytes());
            final URI cd = asURI("SHA-1", digest);
            when(mockDs.getEtagValue()).thenReturn(cd.toString());
        }
        return mockDs;
    }

    public static FedoraBinary mockBinary(final String pid,
                                          final String dsId, final String content) {
        final FedoraBinary mockBinary = mock(FedoraBinary.class);
        when(mockBinary.getPath()).thenReturn("/" + pid + "/" + dsId + "/jcr:content");
        when(mockBinary.getMimeType()).thenReturn("application/octet-stream");
        when(mockBinary.getCreatedDate()).thenReturn(new Date());
        when(mockBinary.getLastModifiedDate()).thenReturn(new Date());
        if (content != null) {
            final MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            final byte[] digest = md.digest(content.getBytes());
            final URI cd = asURI("SHA-1", digest);
            when(mockBinary.getContent()).thenReturn(
                    IOUtils.toInputStream(content));
            when(mockBinary.getContentDigest()).thenReturn(cd);
            when(mockBinary.getEtagValue()).thenReturn(cd.toString());
        }
        return mockBinary;
    }

    private static String getRdfSerialization(final HttpEntity entity) {
        final MediaType mediaType = MediaType.valueOf(entity.getContentType().getValue());
        final Lang lang = contentTypeToLang(mediaType.toString());
        assertNotNull("Entity is not an RDF serialization", lang);
        return lang.getName();
    }

    public static CloseableGraphStore parseTriples(final HttpEntity entity) throws IOException {
        return parseTriples(entity.getContent(), getRdfSerialization(entity));
    }

    public static CloseableGraphStore parseTriples(final InputStream content) {
        return parseTriples(content, "N3");
    }

    public static CloseableGraphStore parseTriples(final InputStream content, final String contentType) {
        final Model model = createDefaultModel();
        model.read(content, "", contentType);
        return new CloseableGraphStore(GraphStoreFactory.create(model));
    }

    /**
     * Set a field via reflection
     *
     * @param parent the owner object of the field
     * @param name the name of the field
     * @param obj the value to set
     */
    public static void setField(final Object parent, final String name, final Object obj) {
        /* check the parent class too if the field could not be found */
        try {
            final Field f = findField(parent.getClass(), name);
            f.setAccessible(true);
            f.set(parent, obj);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(final Class<?> clazz, final String name)
        throws NoSuchFieldException {
        for (final Field f : clazz.getDeclaredFields()) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        if (clazz.getSuperclass() == null) {
            throw new NoSuchFieldException("Field " + name
                    + " could not be found");
        }
        return findField(clazz.getSuperclass(), name);
    }
}
