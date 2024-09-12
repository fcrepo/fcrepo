/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import static java.lang.Long.parseLong;
import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Range header parsing logic
 *
 * @author awoods
 * @author whikloj
 */
public class Range {

    private final long start;

    private final long end;

    private static final Pattern rangePattern =
        compile("^bytes\\s*=\\s*(\\d*)\\s*-\\s*(\\d*)");

    /**
     * Unbounded Range
     */
    private Range() {
        this(-1, -1);
    }

    /**
     * Left-bounded range
     * @param start the start
     */
    public Range(final long start) {
        this(start, -1L);
    }

    /**
     * Left and right bounded range
     * @param start the start
     * @param end the end
     */
    private Range(final long start, final long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Does this range actually impose limits
     * @return true if the range imposes limits
     */
    public boolean hasRange() {
        return !(start == -1 && end == -1);
    }

    /**
     * Length contained in the range
     * @return length of the range
     */
    public long size() {
        if (end == -1) {
            return -1;
        } else if (start == -1) {
            return end;
        }
        return end - start + 1;
    }

    /**
     * Start of the range
     * @return start of the range, or -1 if no start was specified
     */
    public long start() {
        return start;
    }

    /**
     * End of the range
     * @return end of the range
     */
    public long end() {
        return end;
    }

    /**
     * Convert an HTTP Range header to a Range object
     * @param source the source
     * @return range object
     */
    public static Range convert(final String source) {

        final Matcher matcher = rangePattern.matcher(source);

        if (!matcher.matches()) {
            return new Range();
        }

        final String from = matcher.group(1);
        final String to = matcher.group(2);

        final long start;

        if (from.equals("")) {
            start = -1;
        } else {
            start = parseLong(from);
        }

        final long end;
        if (to.equals("")) {
            end = -1;
        } else {
            end = parseLong(to);
        }

        return new Range(start, end);
    }

    /**
     * Create a range object with start and end bytes based on the length of the content.
     * @param length the length of the content
     * @return a range of length object
     */
    public Range.RangeOfLength rangeOfLength(final long length) {
        final long end;

        if (
            end() == -1 || // Range end is not specified
            end() >= length || // Range end is too large
            start() == -1 // Range start is not specified
        ) {
            end = length - 1;
        } else {
            end = end();
        }

        final long start;
        if (start() == -1) {
            start = length - size();
        } else {
            start = start();
        }
        return new RangeOfLength(start, end, length);
    }

    /**
     * Represents a range object based on length
     */
    public static class RangeOfLength {

        /**
         * The start of the range
         */
        private final long start;

        /**
         * The end of the range
         */
        private final long end;

        /**
         * Is the range satisfiable
         */
        private final boolean satisfiable;

        /**
         * Create a range object based on length
         * @param start the start of the range
         * @param end the end of the range
         * @param length the length of the content
         */
        protected RangeOfLength(final long start, final long end, final long length) {
            this.start = start;
            this.end = end;
            // If a valid byte-range-set includes at least one byte-range-spec with a first-byte-pos that is less than
            // the current length of the representation, or at least one suffix-byte-range-spec with a non-zero
            // suffix-length, then the byte-range-set is satisfiable. Otherwise, the byte-range-set is unsatisfiable.
            this.satisfiable = start < length && (end() == -1 || end >= start) && (start != -1 && end != -1);
        }

        /**
         * @return the start of the range
         */
        public long start() {
            return start;
        }

        /**
         * @return the start of the range as a string
         */
        public String startAsString() {
            return Long.toString(start);
        }

        /**
         * @return the end of the range
         */
        public long end() {
            return end;
        }

        /**
         * @return the end of the range as a string
         */
        public String endAsString() {
            return Long.toString(end);
        }

        /**
         * @return true if the range is satisfiable
         */
        public boolean isSatisfiable() {
            return satisfiable;
        }

        /**
         * @return the size of the range, start &amp; end inclusive so 0-0 is size 1
         */
        public long size() {
            return end - start + 1;
        }
    }
}
