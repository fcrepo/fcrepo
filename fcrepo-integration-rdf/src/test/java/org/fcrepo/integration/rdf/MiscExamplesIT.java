/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.rdf;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author cabeer
 * @author ajs6f
 */
@Ignore // TODO FIX THESE TESTS
public class MiscExamplesIT extends AbstractIntegrationRdfIT {


    @Test
    public void testSyntacticallyInvalidDate() {
        createLDPRSAndCheckResponse(getRandomUniqueId(), "<> <info:some/property> \"dunno\"^^<http://www.w3" +
                ".org/2001/XMLSchema#dateTime>");
    }
}
