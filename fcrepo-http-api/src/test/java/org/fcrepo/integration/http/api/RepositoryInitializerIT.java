/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfLexicon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import javax.ws.rs.core.Link;
import java.net.ConnectException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author dbernstein
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        TestIsolationExecutionListener.class,
        DirtyContextBeforeAndAfterClassTestExecutionListener.class
}, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public class RepositoryInitializerIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(RepositoryInitializerIT.class);

    static {
        System.setProperty("fcrepo.autoversioning.enabled", "false");
    }

    @BeforeEach
    public void init() throws Exception {
        // Because of the dirtied context, need to wait for fedora to restart before testing
        int triesRemaining = 50;
        while (true) {
            final HttpGet get = new HttpGet(serverAddress);
            try (final CloseableHttpResponse response = execute(get)) {
                assertEquals(SC_OK, getStatus(response));
                break;
            } catch (final NoHttpResponseException | ConnectException e) {
                if (triesRemaining-- > 0) {
                    LOGGER.debug("Waiting for fedora to become available");
                    Thread.sleep(50);
                } else {
                    throw new Exception("Fedora instance did not become available in allowed time");
                }
            }
        }
        // Now that fedora has started, clear the property so it won't impact other tests
        System.clearProperty("fcrepo.autoversioning.enabled");
    }

    @Test
    public void testRootResourceIsVersioned() throws Exception {
        final var model = getModel("/fcr:versions");
        final var statements = model.listStatements((Resource) null, RdfLexicon.CONTAINS, (RDFNode) null).toList();
        assertEquals(1, statements.size(), "Should be one version contained by time map");
        final var mementURI = statements.get(0).getObject().asResource().getURI();

        assertEquals(1,
                getLinkHeaders(new HttpGet(mementURI)).stream()
                        .map(Link::valueOf)
                        .filter(x -> x.getRel().equals("type"))
                        .filter(x -> x.getUri().toString().equals(MEMENTO_TYPE)).count(),
                "The contained link should be a memento");
    }
}
