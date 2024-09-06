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
 */
public class Range {

    private final long start;

    // Did the Range actually specify a start?
    private final boolean no_start;

    private final long end;

    private static final Pattern rangePattern =
        compile("^bytes\\s*=\\s*(\\d*)\\s*-\\s*(\\d*)");

    /**
     * Unbounded Range
     */
    private Range() {
        this(0, -1);
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
     * @param no_start whether there was a starting byte
     */
    private Range(final long start, final long end, final boolean no_start) {
        this.start = start;
        this.end = end;
        this.no_start = no_start;
    }

    /**
     * Left and right bounded range
     * @param start the start
     * @param end the end
     */
    private Range(final long start, final long end) {
        this(start, end, false);
    }

    /**
     * Does this range actually impose limits
     * @return true if the range imposes limits
     */
    public boolean hasRange() {
        return !(start == 0 && end == -1);
    }

    /**
     * Length contained in the range
     * @return length of the range
     */
    public long size() {
        if (end == -1) {
            return -1;
        } else if (start == 0 && no_start) {
            return end;
        }
        return end - start + 1;
    }

    /**
     * Start of the range
     * @return start of the range, or -1 if no start was specified
     */
    public long start() {
        return no_start ? -1 : start;
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
        final boolean no_start;

        if (from.equals("")) {
            start = 0;
            no_start = true;
        } else {
            start = parseLong(from);
            no_start = false;
        }

        final long end;
        if (to.equals("")) {
            end = -1;
        } else {
            end = parseLong(to);
        }

        return new Range(start, end, no_start);
    }
}
