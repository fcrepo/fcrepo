
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

    public boolean isGood(final FixityResult input) {
        return isGoodFixity.apply(input);
    }

    @Override
    public Set<FixityResult> apply(final Collection<FixityResult> input) {
        return copyOf(filter(input, isGoodFixity));
    }

    static Predicate<FixityResult> isGoodFixity =
            new Predicate<FixityResult>() {

                @Override
                public boolean apply(final FixityResult input) {
                    return input.matches();
                }
            };
}
