/**
 * Copyright 2013 DuraSpace, Inc.
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
/**
 *
 */

package org.fcrepo.kernel.identifiers;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Splitter.fixedLength;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;

import java.util.List;
import com.google.common.base.Function;

/**
 * Injects and extracts segments of hierarchy before the last segment of a
 * multi-part identifier to ensure efficient performance of the JCR.
 *
 * @author ajs6f
 * @date Mar 26, 2014
 */
public class HierarchyTranslator extends InternalIdentifierTranslator {

    private String separator;

    private String prefix;

    private final Function<String, String> addPrefix = new Function<String, String>() {

        @Override
        public String apply(final String input) {
            return prefix + input;
        }
    };

    private static final int DEFAULT_LENGTH = 2;

    private static final int DEFAULT_COUNT = 4;

    private int length = DEFAULT_LENGTH;

    private int levels = DEFAULT_COUNT;

    /*
     * (non-Javadoc)
     * @see com.google.common.base.Converter#doForward(java.lang.Object)
     */
    @Override
    protected String doForward(final String flat) {
        // adds hierarchy
        final List<String> hierarchySegments = createHierarchySegments();
        final List<String> flatSegments = asList(flat.split(separator));

        final Iterable<String> lastSegment = singletonList(getLast(flatSegments));
        final Iterable<String> firstSegments = flatSegments.subList(0, flatSegments.size() - 1);
        final Iterable<String> allSegments = concat(firstSegments, hierarchySegments, lastSegment);
        return on(separator).join(allSegments);
    }

    /*
     * (non-Javadoc)
     * @see com.google.common.base.Converter#doBackward(java.lang.Object)
     */
    @Override
    protected String doBackward(final String hierarchical) {
        // removes hierarchy
        final List<String> segments = asList(hierarchical.split(separator));
        // we subtract one for the final segment, then levels for the inserted
        // hierarchy segments we want to remove
        final List<String> firstSegments = segments.subList(0, segments.size() - 1 - levels);
        final List<String> lastSegment = singletonList(getLast(segments));
        return on(separator).join(concat(firstSegments, lastSegment));
    }

    private List<String> createHierarchySegments() {
        // offers a list of segments sliced out of a UUID, each prefixed
        if (levels == 0) {
            return emptyList();
        }
        return transform(fixedLength(length).splitToList(createHierarchyCharacterBlock()), addPrefix);
    }

    private CharSequence createHierarchyCharacterBlock() {
        return randomUUID().toString().replace("-", "").substring(0, length * levels);
    }

    /**
     * @param separator the separator to use
     */
    public void setSeparator(final String sep) {
        this.separator = sep;
    }

    /**
     * @param length the length to set
     */
    public void setLength(final int l) {
        if (l < 1) {
            throw new IllegalArgumentException("Segment length must be at least one!");
        }
        this.length = l;
    }

    /**
     * @param levels the levels to set
     */
    public void setLevels(final int c) {
        this.levels = c;
    }

    /**
     * @param prefix the prefix to set
     */
    public void setPrefix(final String p) {
        this.prefix = p;
    }

}
