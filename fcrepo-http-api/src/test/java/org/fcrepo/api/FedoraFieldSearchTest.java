
package org.fcrepo.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.jaxb.search.FieldSearchResult;
import org.fcrepo.jaxb.search.ObjectFields;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraFieldSearchTest {

    FedoraFieldSearch testObj;

    Session mockSession;
    private NodeService mockNodeService;
    private UriInfo uriInfo;

    @Before
    public void setUp() throws LoginException, RepositoryException {
        testObj = new FedoraFieldSearch();
        mockSession = TestHelpers.mockSession(testObj);
        uriInfo = TestHelpers.getUriInfoImpl();
        testObj.setUriInfo(uriInfo);
        mockNodeService = mock(NodeService.class);
        testObj.setNodeService(mockNodeService);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testFieldSearch() throws RepositoryException, URISyntaxException {

        final Request mockRequest = mock(Request.class);

        when(uriInfo.getRequestUri()).thenReturn(new URI("http://localhost/fcrepo/path/to/query/endpoint"));
        when(mockRequest.selectVariant(any(List.class))).thenReturn(
                                                                           new Variant(MediaType.valueOf("application/n-triples"), null,
                                                                                              null));

        testObj.searchSubmitRdf("ZZZ", 0, 0, mockRequest);

        verify(mockNodeService).searchRepository(any(GraphSubjects.class), eq(ResourceFactory.createResource("http://localhost/fcrepo/path/to/query/endpoint")), eq(mockSession), eq("ZZZ"), eq(0), eq(0L));
    }

}
