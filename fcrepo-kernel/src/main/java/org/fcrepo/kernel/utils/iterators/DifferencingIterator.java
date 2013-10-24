
package org.fcrepo.kernel.utils.iterators;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import com.google.common.base.Predicate;

/**
 * An {@link Iterator} that splits its produced elements between two
 * {@link Queue}s. One contains all elements that matched against a provided
 * {@link Set}, the other contains those that did not.
 *
 * @author ajs6f
 * @date Oct 23, 2013
 * @param <T>
 */
public class DifferencingIterator<T> {

    private Predicate<T> toBeMatched;

    private Queue<T> matched;

    private Queue<T> notMatched;

    private Iterator<T> source;

    public DifferencingIterator(final Predicate<T> toBeMatched,
            final Iterator<T> source) {
        this.toBeMatched = toBeMatched;
        this.source = source;
    }

    public void divide() {
        while (source.hasNext()) {
            final T next = source.next();
            if (toBeMatched.apply(next)) {
                matched.add(next);
            } else {
                notMatched.add(next);
            }
        }
    }

    /**
     * This method will return {@code null} until after the wrapped
     * {@link Iterator} is exhausted.
     *
     * @return A {@link Queue} of those elements that matched against the
     *         provided {@link Set}
     */
    public Queue<T> matched() {
        return source.hasNext() ? null : matched;
    }

    /**
     * This method will return {@code null} until after the wrapped
     * {@link Iterator} is exhausted.
     *
     * @return A {@link Queue} of those elements that did not match against the
     *         provided {@link Set}
     */
    public Queue<T> notMatched() {
        return source.hasNext() ? null : notMatched;
    }

}
