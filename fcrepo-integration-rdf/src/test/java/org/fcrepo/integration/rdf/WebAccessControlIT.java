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
package org.fcrepo.integration.rdf;

import org.junit.Test;

import java.io.IOException;

/**
 * @author cabeer
 */
public class WebAccessControlIT extends AbstractIntegrationRdfIT {
    @Test
    public void testExample() throws IOException {
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
