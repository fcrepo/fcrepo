/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.rdf;

import java.io.IOException;

import org.junit.Test;

/**
 * @author cabeer
 */
 public class B3KatIT extends AbstractIntegrationRdfIT {
    @Test
    public void testExampleRecord() throws IOException {
        createLDPRSAndCheckResponse(getRandomUniqueId(), getContentFromClasspath("/examples/b3kat.ttl"));
    }
}