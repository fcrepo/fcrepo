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
