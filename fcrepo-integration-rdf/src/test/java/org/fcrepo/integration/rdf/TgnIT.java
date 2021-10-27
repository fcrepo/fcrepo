/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.rdf;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * @author cabeer
 */
@Ignore // TODO FIX THESE TESTS
public class TgnIT extends AbstractIntegrationRdfIT {
    @Test
    public void testExampleRecord() throws IOException {
        createLDPRSAndCheckResponse(getRandomUniqueId(), getContentFromClasspath("/examples/tgn.ttl"));
    }
}