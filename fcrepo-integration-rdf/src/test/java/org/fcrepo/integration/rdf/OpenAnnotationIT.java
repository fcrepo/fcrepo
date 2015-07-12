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
 */
public class OpenAnnotationIT extends AbstractIntegrationRdfIT {
    @Test
    public void testOpenAnnotationChoice() {
        final String s = "@prefix content: <http://www.w3.org/2011/content#> .\n" +
                "@prefix dc11: <http://purl.org/dc/elements/1.1/> .\n" +
                "@prefix dcmitype: <http://purl.org/dc/dcmitype/> .\n" +
                "@prefix openannotation: <http://www.w3.org/ns/oa#> .\n" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "\n" +
                "<> a openannotation:Annotation;\n" +
                "   openannotation:hasBody [\n" +
                "     a openannotation:Choice;\n" +
                "     openannotation:default [\n" +
                "       a content:ContentAsText,\n" +
                "         dcmitype:Text;\n" +
                "       dc11:language \"en\";\n" +
                "       content:chars \"I love this English!\"\n" +
                "     ];\n" +
                "     openannotation:item [\n" +
                "       a content:ContentAsText,\n" +
                "         dcmitype:Text;\n" +
                "       dc11:language \"fr\";\n" +
                "       content:chars \"Je l'aime en Francais!\"\n" +
                "     ]\n" +
                "   ];\n" +
                "   openannotation:hasTarget <http://purl.stanford.edu/kq131cs7229>;\n" +
                "   openannotation:motivatedBy openannotation:commenting .";
        createLDPRSAndCheckResponse(getRandomUniqueId(), s);
    }
}
