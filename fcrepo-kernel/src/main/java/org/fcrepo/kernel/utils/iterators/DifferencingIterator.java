
package org.fcrepo.kernel.utils.iterators;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.AbstractIterator;

/**
 * A wrapping {@link Iterator} that filters any element that appears in a set
 *
 * @author ajs6f
 * @date Oct 24, 2013
 * @param <E>
 */
public class DifferencingIterator<E> extends AbstractIterator<E> {

    Set<? extends E> notCommon;

    private Set<E> common;

    private Iterator<E> source;

    /**
     * Ordinary constructor.
     *
     * @param notCommon
     * @param common
     * @param source
     */
    public DifferencingIterator(final Set<? extends E> original,
            final Iterator<E> source) {
        super();
        this.notCommon = newHashSet(original);
        this.common = newHashSet();
        this.source = source;
    }

    @Override
    protected E computeNext() {
        if (source.hasNext()) {
            E next = source.next();
            // we only want to return this element if it is not common
            // to the two inputs
            while (common.contains(next) || notCommon.contains(next)) {
                // it was common, so shift it to common
                if (notCommon.remove(next)) {
                    common.add(next);
                }
                // move onto the next candidate
                if (!source.hasNext()) {
                    return endOfData();
                } else {
                    next = source.next();
                }
            }
            // it was not common so return it
            return next;
        } else {
            return endOfData();
        }
    }

    /**
     * This method will return null until the source iterator is exhausted.
     *
     * @return The elements that turned out to be common to the two inputs.
     */
    public Set<E> common() {
        return source.hasNext() ? null : common;
    }

    /**
     * This method will return null until the source iterator is exhausted.
     *
     * @return The elements that turned out not to be common to the two inputs.
     */
    public Set<? extends E> notCommon() {
        return source.hasNext() ? null : notCommon;
    }

}
