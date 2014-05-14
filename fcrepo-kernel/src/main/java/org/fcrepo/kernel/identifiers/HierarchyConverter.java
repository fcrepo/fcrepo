/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.identifiers;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Splitter.fixedLength;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.base.Function;

/**
 * Injects and extracts segments of hierarchy in a multi-part identifier
 * to ensure efficient performance of the JCR.
 *
 * @author ajs6f
 * @date Mar 26, 2014
 * @author lsitu
 * @date May 9, 2014
 */
public class HierarchyConverter extends InternalIdentifierConverter {

    public static final String DEFAULT_SEPARATOR = "/";

    private String separator = DEFAULT_SEPARATOR;

    private String prefix = "";

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

    private static final Logger log = getLogger(HierarchyConverter.class);

    /*
     * Convert an incoming path to auto-hierarchy path for JCR backend storage.
     * For example: /parent/child => /xx/xx/parent/xx/xx/child
     * @see com.google.common.base.Converter#doBackward(java.lang.Object)
     */
    @Override
    protected String doBackward(final String flat) {
        log.debug("Converting incoming identifier: {}", flat);
        String nonContentSuffixed = flat;
        final boolean isContentFile = flat.endsWith(FCR_CONTENT) || flat.endsWith(JCR_CONTENT);
        if (nonContentSuffixed.startsWith(separator)) {
            nonContentSuffixed = nonContentSuffixed.substring(1, nonContentSuffixed.length());
        }
        final List<String> flatSegments = asList(nonContentSuffixed.split(separator));
        List<String> hierarchySegments = null;
        final List<String> jcrPathSegments = new ArrayList<>();
        int pathSize = flatSegments.size();
        if (isContentFile) {
            jcrPathSegments.add(JCR_CONTENT);
            // Subtract the fcr:content namespace element for auto-hierarchy
            pathSize -= 1;
        }
        if (pathSize == 0) {
            // either empty identifier or separator identifier
            return nonContentSuffixed;
        }
        for (int i = pathSize - 1; i >= 0; i--) {
            final String seg = flatSegments.get(i);
            if (seg == null || seg.length() == 0) {
                if (pathSize == 1) {
                    return flat;
                }
            } else {
                // Create the auto-hierarchy path segments, add them to
                // the list in front of the transparent path segment
                hierarchySegments = createHierarchySegments(hashKey(singletonList(seg)));
                jcrPathSegments.add(0, flatSegments.get(i));
                jcrPathSegments.addAll(0, hierarchySegments);
            }
        }
        final String pathConverted = on(separator).join(jcrPathSegments);
        log.trace("Converted incoming identifier \"{}\" to \"{}\".", flat, pathConverted);
        return "/" + pathConverted;
    }

    /*
     * Convert an outgoing JCR hierarchy path to transparent path.
     * For example: /xx/xx/parent/xx/xx/child => /parent/child
     * @see com.google.common.base.Converter#doForward(java.lang.Object)
     */
    @Override
    protected String doForward(final String hierarchical) {
        log.debug("Converting outgoing identifier: {}", hierarchical);
        String nonContentSuffixed = hierarchical;
        if (nonContentSuffixed.startsWith(separator)) {
            nonContentSuffixed = nonContentSuffixed.substring(1, nonContentSuffixed.length());
        }
        final List<String> jcrPathSegments = asList(nonContentSuffixed.split(separator));
        int jcrPathSize = jcrPathSegments.size();
        final boolean isContentFile = hierarchical.endsWith(JCR_CONTENT);
        if (isContentFile) {
            jcrPathSize -= 1;
        }
        if (jcrPathSize <= levels) {
            // must be a root identifier
            return hierarchical;
        }
        // Convert the auto-hierarchy path to transparent path
        final List<String> pathSegments = new ArrayList<>();
        List<String> hierarchySegments = null;
        for (int i = levels ; i < jcrPathSize; i += levels + 1) {
            final String seg = jcrPathSegments.get(i);
            if (seg == null || seg.length() == 0) {
                // If the segment null or empty, must be double slash or something wrong.
                // Add it as empty that will produce an slash for now?
                pathSegments.add("");
            } else {
                pathSegments.add(seg);
                hierarchySegments = jcrPathSegments.subList(i - levels, i);
                final String hierarchyPath = on(separator).join(hierarchySegments);
                final String hashPath = on(separator).join(createHierarchySegments(seg));
                if (!hierarchyPath.equals(hashPath)) {
                    log.error("Outgoing sub path {} hierarchy {} (got {}) doesn't match the path segament {}.",
                            on(separator).join(jcrPathSegments.subList(0, i + 1)), hierarchyPath, hashPath, seg);
                }
            }
        }

        if (isContentFile) {
            //Don't forget the fcr:content segment for content files
            pathSegments.add(FCR_CONTENT);
        }
        final String pathConverted = on(separator).join(pathSegments);
        log.trace("Converted outgoing identifier \"{}\" to \"{}\".", hierarchical, pathConverted);
        return "/" + pathConverted;
    }

    private List<String> createHierarchySegments(final String path) {
        // offers a list of segments sliced out of a UUID, each prefixed
        if (levels == 0) {
            return emptyList();
        }
        return transform(fixedLength(length).splitToList(createHierarchyCharacterBlock(path)), addPrefix);
    }

    private String hashKey(final List<String> pathSegments) {
        return on(separator).join(pathSegments);
    }

    private CharSequence createHierarchyCharacterBlock(final String path) {
        String hierarchyPath = "";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
            md.reset();
            md.update(path.getBytes("utf8"));
            hierarchyPath = byteToHex(md.digest()).replace("-", "").substring(0, length * levels);
        } catch (final Exception e) {
            log.error("Hierarchy conversion auto-hierarchy", e);
        }
        return hierarchyPath;
    }

    private static String byteToHex(final byte[] hash) {
        final Formatter formatter = new Formatter();
        for (final byte b : hash) {
            formatter.format("%02x", b);
        }
        final String result = formatter.toString();
        formatter.close();
        return result.toLowerCase();
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
