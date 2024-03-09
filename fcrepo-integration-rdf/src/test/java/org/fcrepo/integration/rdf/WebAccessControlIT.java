/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.rdf;

import org.junit.Test;

/**
 * @author cabeer
 */
public class WebAccessControlIT extends AbstractIntegrationRdfIT {
    @Test
    public void testExample() {
        final String pid = getRandomUniqueId();
        final String card = createObject(pid + "-card").getFirstHeader("Location").getValue();
        final String s = "@prefix acl: <http://www.w3.org/ns/auth/acl#> . \n" +
                "@prefix foaf: <http://xmlns.com/foaf/0.1/> . \n" +
                "\n" +
                "<> acl:accessTo <" + card + ">;  \n" +
                "   acl:mode acl:Read, acl:Write; \n" +
                "   acl:agent <" + card + "#me> .";

        createLDPRSAndCheckResponse(getRandomUniqueId(), s);
    }
}
