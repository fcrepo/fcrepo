
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;
import com.google.common.base.Function;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

public class FedoraIdentifiersTest {

    private static final Logger LOGGER = getLogger(FedoraIdentifiersTest.class);

    private PidMinter mockPidMinter;

    private FedoraIdentifiers testObj;

    private UriInfo uriInfo;
    private Session mockSession;

    @Before
    public void initMocks() throws RepositoryException {
        testObj = new FedoraIdentifiers();

        mockPidMinter = mock(PidMinter.class);

        mockSession = TestHelpers.mockSession(testObj);

        uriInfo = TestHelpers.getUriInfoImpl();
        testObj.setUriInfo(uriInfo);

    }

    @Test
    public void testGetNextPid() throws RepositoryException, URISyntaxException {
        when(mockPidMinter.makePid()).thenReturn(
                new Function<Object, String>() {

                    @Override
                    public String apply(final Object input) {
                        return "asdf:123";
                    }
                });


        testObj.setPidMinter(mockPidMinter);

        when(uriInfo.getAbsolutePath()).thenReturn(new URI("http://localhost/fcrepo/rest/fcr:pid"));

        final Dataset np = testObj.getNextPid(createPathList(""), 2, uriInfo);

        LOGGER.debug("Got dataset {}", np.getDefaultModel().toString());
        assertTrue(np.getDefaultModel().contains(ResourceFactory.createResource("http://localhost/fcrepo/rest/fcr:pid"), ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasMember"), ResourceFactory.createResource("http://localhost/fcrepo/rest/asdf:123")));

    }
}
