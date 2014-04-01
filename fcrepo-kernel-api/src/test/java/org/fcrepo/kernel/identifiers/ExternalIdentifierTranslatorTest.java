
package org.fcrepo.kernel.identifiers;

import static java.util.Collections.singletonList;
import static org.fcrepo.kernel.identifiers.InternalIdentifierTranslator.identityTranslation;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * @author ajs6f
 * @date Apr 1, 2014
 */
public class ExternalIdentifierTranslatorTest {

    private ExternalIdentifierTranslator testExternalIdentifierTranslator;

    private final String testId = "test1/test2";

    @Before
    public void setUp() {
        testExternalIdentifierTranslator = new ExternalIdentifierTranslator();
    }

    @Test
    public void testNoop() {
        testExternalIdentifierTranslator.setTranslationChain(singletonList(identityTranslation()));
        assertEquals("Should have received our original identifier back!", testId, testExternalIdentifierTranslator
                .reverse().convert(testExternalIdentifierTranslator.convert(testId)));
    }

}
