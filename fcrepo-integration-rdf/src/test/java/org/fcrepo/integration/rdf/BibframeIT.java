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

/**
 * @author cabeer
 * @author ajs6f
 */
public class BibframeIT extends AbstractIntegrationRdfIT {

    @Test
    public void testBibframe() {
        final String bibframe = "@prefix bf: <http://bibframe.org/vocab/> .\n" +
                "@prefix madsrdf: <http://www.loc.gov/mads/rdf/v1#> .\n" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix relators: <http://id.loc.gov/vocabulary/relators/> .\n" +
                "@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "\n" +
                "<> a bf:Work ;\n" +
                "    bf:hasInstance [ a bf:Electronic,\n" +
                "                bf:Instance ;\n" +
                "            bf:instanceOf <http://id.loc.gov/resources/bibs/15716616> ;\n" +
                "            bf:label \"Electronic Resource\" ;\n" +
                "            bf:uri <http://www.soaw.org/new/newsletter.php> ],\n" +
                "        [ a bf:Instance,\n" +
                "                bf:Serial ;\n" +
                "            bf:derivedFrom <http://id.loc.gov/resources/bibs/15716616.marcxml.xml> ;\n" +
                "            bf:instanceOf <http://id.loc.gov/resources/bibs/15716616> ;\n" +
                "            bf:instanceTitle [ a bf:Title ] ;\n" +
                "            bf:issn [ a bf:Identifier ;\n" +
                "                    bf:identifierAssigner \"1\" ;\n" +
                "                    bf:identifierScheme \"issn\" ;\n" +
                "                    bf:identifierValue \"1949-3223\" ] ;\n" +
                "            bf:keyTitle [ a bf:Title ] ;\n" +
                "            bf:modeOfIssuance \"serial\" ;\n" +
                "            bf:note \"\\\"Newspaper of the movement to close the School of the Americas.\\\"\",\n" +
                "                \"Latest issue consulted: Vol. 14, no. 1 (winter/spring 2009).\",\n" +
                "                \"Title from caption.\" ;\n" +
                "            bf:otherPhysicalFormat [ a bf:Instance ;\n" +
                "                    bf:title \"¡Presente!\" ] ;\n" +
                "            bf:publication [ a bf:Provider ;\n" +
                "                    bf:providerDate \"[2006]-\" ;\n" +
                "                    bf:providerName [ a bf:Organization ;\n" +
                "                            bf:label \"SOA Watch\" ] ;\n" +
                "                    bf:providerPlace [ a bf:Place ;\n" +
                "                            bf:label \"Washington, D.C. \" ] ] ;\n" +
                "            bf:serialFirstIssue \"Vol. 11, issue 3 (fall 2006)\" ;\n" +
                "            bf:stockNumber [ a bf:Identifier ;\n" +
                "                    bf:identifierAssigner \"SOA Watch, P.O. Box 4566, Washington, DC 20017\" ;\n" +
                "                    bf:identifierScheme \"stockNumber\" ] ] .";

        createLDPRSAndCheckResponse(getRandomUniqueId(), bibframe);
    }

}
