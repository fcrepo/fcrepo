/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.rdf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * @author cabeer
 */
public class BibOntologyIT extends AbstractIntegrationRdfIT {
    @Test
    public void testExampleRecord() throws IOException {
        createLDPRSAndCheckResponse(getRandomUniqueId(), getContentFromClasspath("/examples/bibontology.ttl"));
    }
}
