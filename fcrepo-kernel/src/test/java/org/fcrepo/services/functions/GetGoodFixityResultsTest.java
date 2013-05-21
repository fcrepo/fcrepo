
package org.fcrepo.services.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        when(mockFixity.matches()).thenReturn(true, false);
        assertTrue(testPred.apply(mockFixity));
        assertFalse(testPred.apply(mockFixity));
    }

    @Test
    public void testIsGood() {
        final GetGoodFixityResults testObj = new GetGoodFixityResults();
        final FixityResult mockFixity = mock(FixityResult.class);

        when(mockFixity.matches()).thenReturn(true, false);
        assertTrue(testObj.isGood(mockFixity));
        assertFalse(testObj.isGood(mockFixity));
    }

    @Test
    public void testFunction() {
        final GetGoodFixityResults testObj = new GetGoodFixityResults();
        final FixityResult mockBadFixity = mock(FixityResult.class);
        final FixityResult mockGoodFixity = mock(FixityResult.class);
        final List<FixityResult> mockList =
                Arrays.asList(new FixityResult[] {mockBadFixity, mockGoodFixity});
        when(mockGoodFixity.matches()).thenReturn(true);
        when(mockBadFixity.matches()).thenReturn(false);
        final Set<FixityResult> actual = testObj.apply(mockList);
        assertEquals(1, actual.size());
        assertTrue(actual.contains(mockGoodFixity));
    }
}
