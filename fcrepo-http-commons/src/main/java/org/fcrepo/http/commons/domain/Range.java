/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private final long end;

    private static Pattern rangePattern =
        compile("^bytes\\s*=\\s*(\\d*)\\s*-\\s*(\\d*)");

    /**
     * Unbounded Range
     */
    public Range() {
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
     */
    public Range(final long start, final long end) {
        this.start = start;
        this.end = end;
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
        }
        return end - start + 1;
    }

    /**
     * Start of the range
     * @return start of the range
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
            start = 0;
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
}
