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
package org.fcrepo.kernel.modeshape.identifiers;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Splitter.fixedLength;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.fcrepo.kernel.api.identifiers.InternalIdentifierConverter;
import org.slf4j.Logger;

/**
 * Injects and extracts segments of hierarchy before the last segment of a
 * multi-part identifier to ensure efficient performance of the JCR.
 *
 * @author ajs6f
 * @since Mar 26, 2014
 */
public class HierarchyConverter extends InternalIdentifierConverter {

    public static final String DEFAULT_SEPARATOR = "/";

    private String separator = DEFAULT_SEPARATOR;

    private String prefix = "";

    private static final int DEFAULT_LENGTH = 2;

    private static final int DEFAULT_COUNT = 4;

    private int length = DEFAULT_LENGTH;

    private int levels = DEFAULT_COUNT;

    private static final Logger log = getLogger(HierarchyConverter.class);

    /*
     * (non-Javadoc)
     * @see com.google.common.base.Converter#doBackward(java.lang.Object)
     */
    @Override
    protected String doBackward(final String flat) {
        log.debug("Converting incoming identifier: {}", flat);
        final List<String> hierarchySegments = createHierarchySegments();
        final List<String> flatSegments = asList(flat.split(separator));
        List<String> firstSegments = emptyList();
        List<String> lastSegment = emptyList();
        if (flatSegments.size() == 0) {
            // either empty identifier or separator identifier
            return on(separator).join(hierarchySegments);
        }
        if (flatSegments.size() > 1) {
            lastSegment = singletonList(getLast(flatSegments));
            firstSegments = flatSegments.subList(0, flatSegments.size() - 1);
        } else {
            // just one segment
            lastSegment = singletonList(flatSegments.get(0));
        }
        final Iterable<String> allSegments = concat(firstSegments, hierarchySegments, lastSegment);

        return on(separator).join(allSegments);
    }

    /*
     * (non-Javadoc)
     * @see com.google.common.base.Converter#doForward(java.lang.Object)
     */
    @Override
    protected String doForward(final String hierarchical) {
        log.debug("Converting outgoing identifier: {}", hierarchical);
        final List<String> segments = asList(hierarchical.split(separator));
        if (segments.size() <= levels) {
            // must be a root identifier
            return "";
        }
        List<String> firstSegments = emptyList();
        List<String> lastSegment = emptyList();
        if (segments.size() > levels + 1) {
            // we subtract one for the final segment, then levels for the
            // inserted hierarchy segments we want to remove
            firstSegments = segments.subList(0, segments.size() - 1 - levels);
            lastSegment = singletonList(getLast(segments));
        } else {
            // just the trailing non-hierarchical segment
            lastSegment = singletonList(getLast(segments));
        }
        return on(separator).join(concat(firstSegments, lastSegment));
    }

    private List<String> createHierarchySegments() {
        // offers a list of segments sliced out of a UUID, each prefixed
        if (levels == 0) {
            return emptyList();
        }
        return transform(fixedLength(length).splitToList(createHierarchyCharacterBlock()), x -> prefix + x);
    }

    private CharSequence createHierarchyCharacterBlock() {
        return randomUUID().toString().replace("-", "").substring(0, length * levels);
    }

    /**
     * @param sep the separator to use
     */
    public void setSeparator(final String sep) {
        this.separator = sep;
    }

    /**
     * @param l the length to set
     */
    public void setLength(final int l) {
        if (l < 1) {
            throw new IllegalArgumentException("Segment length must be at least one!");
        }
        this.length = l;
    }

    /**
     * @param l the levels to set
     */
    public void setLevels(final int l) {
        this.levels = l;
    }

    /**
     * @param p the prefix to set
     */
    public void setPrefix(final String p) {
        this.prefix = p;
    }

}
