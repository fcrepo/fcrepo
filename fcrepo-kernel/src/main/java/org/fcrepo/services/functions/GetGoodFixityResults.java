
package org.fcrepo.services.functions;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.ImmutableSet.copyOf;

import java.util.Collection;
import java.util.Set;

import org.fcrepo.utils.FixityResult;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class GetGoodFixityResults implements
        Function<Collection<FixityResult>, Set<FixityResult>> {

    IsGoodFixity predicate = new IsGoodFixity();

    public boolean isGood(FixityResult input) {
        return predicate.apply(input);
    }

    @Override
    public Set<FixityResult> apply(Collection<FixityResult> input) {
        return copyOf(filter(input, predicate));
    }

    static class IsGoodFixity implements Predicate<FixityResult> {

        @Override
        public boolean apply(FixityResult input) {
            return input.computedChecksum.equals(input.dsChecksum) &&
                    input.computedSize == input.dsSize;
        }

    }

}
