
package org.fcrepo.services.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.fcrepo.utils.FixityResult;
import org.junit.Test;

import com.google.common.base.Predicate;

public class GetGoodFixityResultsTest {

    @Test
    public void testPredicate() {
        final Predicate<FixityResult> testPred =
                GetGoodFixityResults.isGoodFixity;
        final FixityResult mockFixity = mock(FixityResult.class);
        final URI uri = URI.create("urn:foo:bar");
        final URI otherUri = URI.create("urn:does:not:match");
        mockFixity.computedChecksum = uri;
        mockFixity.dsChecksum = otherUri;
        assertFalse(testPred.apply(mockFixity));
        mockFixity.dsChecksum = uri;
        mockFixity.computedSize = 1L;
        assertFalse(testPred.apply(mockFixity));
        mockFixity.dsSize = 1L;
        assertTrue(testPred.apply(mockFixity));
    }

    @Test
    public void testIsGood() {
        final GetGoodFixityResults testObj = new GetGoodFixityResults();
        final FixityResult mockFixity = mock(FixityResult.class);
        final URI uri = URI.create("urn:foo:bar");
        final URI otherUri = URI.create("urn:does:not:match");
        mockFixity.computedChecksum = uri;
        mockFixity.dsChecksum = otherUri;
        assertFalse(testObj.isGood(mockFixity));
        mockFixity.dsChecksum = uri;
        mockFixity.computedSize = 1L;
        assertFalse(testObj.isGood(mockFixity));
        mockFixity.dsSize = 1L;
        assertTrue(testObj.isGood(mockFixity));
    }

    @Test
    public void testFunction() {
        final GetGoodFixityResults testObj = new GetGoodFixityResults();
        final FixityResult mockBadFixity = mock(FixityResult.class);
        final FixityResult mockGoodFixity = mock(FixityResult.class);
        final List<FixityResult> mockList =
                Arrays.asList(new FixityResult[] {mockBadFixity, mockGoodFixity});
        final URI uri = URI.create("urn:foo:bar");
        final URI otherUri = URI.create("urn:does:not:match");
        mockBadFixity.computedChecksum = uri;
        mockBadFixity.dsChecksum = otherUri;
        mockGoodFixity.computedChecksum = uri;
        mockGoodFixity.dsChecksum = uri;
        final Set<FixityResult> actual = testObj.apply(mockList);
        assertEquals(1, actual.size());
        assertTrue(actual.contains(mockGoodFixity));
    }
}
