/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api;

import static org.apache.jena.vocabulary.DC_11.title;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * <p>RdfLexiconTest class.</p>
 *
 * @author ajs6f
 * @author whikloj
 */
public class RdfLexiconTest {

    @Test
    public void repoPredicatesAreManaged() {
        assertTrue(isManagedPredicate.test(HAS_MESSAGE_DIGEST));
        assertTrue(isManagedPredicate.test(CREATED_BY));
    }
    @Test
    public void otherPredicatesAreNotManaged() {
        assertFalse(isManagedPredicate.test(title));
    }
}
