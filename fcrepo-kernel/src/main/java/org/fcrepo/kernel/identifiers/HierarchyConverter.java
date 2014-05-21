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
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.isBlank;
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
 * @author lsitu
 * @since Mar 26, 2014
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

    private static final String JCR_NAMESPACE_PREFIX = "jcr";
    private static final String FCR_NAMESPACE_PREFIX = "fcr";

    /*
     * Convert an incoming path to auto-hierarchy path for JCR backend storage.
     * For example: /parent/child => /xx/xx/parent/xx/xx/child
     * @see com.google.common.base.Converter#doBackward(java.lang.Object)
     */
    @Override
    protected String doBackward(final String flat) {
        log.debug("Converting incoming identifier: {}", flat);
        String nonContentSuffixed = flat;
        if (nonContentSuffixed.startsWith(separator)) {
            nonContentSuffixed = nonContentSuffixed.substring(1, nonContentSuffixed.length());
        }
        final List<String> flatSegments = asList(nonContentSuffixed.split(separator));
        List<String> hierarchySegments = null;
        final List<String> jcrPathSegments = new ArrayList<>();
        final int pathSize = flatSegments.size();
        if (pathSize == 0) {
            // either empty identifier or separator identifier
            return nonContentSuffixed;
        }
        boolean isVersionRelate = false;
        for (String seg : flatSegments) {
            if (isBlank(seg)) {
                if (pathSize == 1) {
                    return flat;
                }
            } else if (isVersionRelate) {
                // Skip translation after version syntax detected for now
                jcrPathSegments.add(seg);
            } else if (isFCRNamespace(seg)) {
                // FCR related namspaces
                jcrPathSegments.add(convertNamespace(seg));
                if (seg.indexOf(":versionStorage") >= 0 || seg.indexOf(":versions") > 0)  {
                    isVersionRelate = true;
                }
            } else if (seg.startsWith("[") && seg.endsWith("]")) {
                // System related syntax?
                jcrPathSegments.add(seg);
            } else {
                // Create the auto-hierarchy path segments, add them to
                // the list in front of the transparent path segment
                hierarchySegments = createHierarchySegments(hashKey(singletonList(seg)));
                jcrPathSegments.addAll(hierarchySegments);
                jcrPathSegments.add(seg);
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
        final int jcrPathSize = jcrPathSegments.size();
        if (jcrPathSize <= levels) {
            // must be a root identifier
            return hierarchical;
        }
        boolean isVersionRelate = false;
        // Convert the auto-hierarchy path to transparent path
        final List<String> pathSegments = new ArrayList<>();
        final List<String> hierarchySegments = new ArrayList<>();
        for (int i = 0 ; i < jcrPathSize; i++) {
            final String seg = jcrPathSegments.get(i);
            final int hierarchySize = hierarchySegments.size();
            if (isBlank(seg)) {
                // If the segment null or empty, must be double slash or something wrong.
                // Add it as empty that will produce an slash for now?
                pathSegments.add("");
                hierarchySegments.clear();
            } else if (isVersionRelate) {
                // Skip translation after version syntax detected for now
                pathSegments.add(seg);
            } else if (isJCRNamespace(seg)) {
                // Convert namespace
                pathSegments.add(convertNamespace(seg));
                if (seg.indexOf(":versionStorage") >= 0 || seg.indexOf(":versions") > 0)  {
                    isVersionRelate = true;
                }
                if (hierarchySize > 0) {
                    // Something wrong
                    log.error("Outgoing hierarchy {} in path {} has no match.",
                            on(separator).join(hierarchySegments), jcrPathSegments.subList(0, i));
                    hierarchySegments.clear();
                }
            } else if (hierarchySize == levels) {
                // Completed a hierarchy sub-part conversion
                pathSegments.add(seg);
                final String hierarchyPath = on(separator).join(hierarchySegments);
                final String hashPath = on(separator).join(createHierarchySegments(seg));
                if (!hierarchyPath.equals(hashPath)) {
                    log.error("Outgoing sub path {} hierarchy {} (got {}) doesn't match the path segment {}.",
                            on(separator).join(jcrPathSegments.subList(0, i + 1)), hierarchyPath, hashPath, seg);
                }
                // Clear the hierarchy segments
                hierarchySegments.clear();
            } else {
                // A hierarchy segment
                hierarchySegments.add(seg);
            }

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
            hierarchyPath = byteToHex(md.digest()).substring(0, length * levels);
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

    /**
     * @param value
     * @return
     */
    public boolean isJCRNamespace(final String value) {
        return value.split(":")[0].equals(JCR_NAMESPACE_PREFIX);
    }

    /**
     * @param value
     * @return
     */
    public boolean isFCRNamespace(final String value) {
        return value.split(":")[0].equals(FCR_NAMESPACE_PREFIX);
    }

    /**
     * Convert namespaces
     * @param namespace
     * @return
     */
    public String convertNamespace(final String namespace) {
        final String[] tokens = namespace.split(":");
        final int length = tokens.length;
        if (tokens[0].equals(JCR_NAMESPACE_PREFIX) && length == 2 ) {
            return FCR_NAMESPACE_PREFIX + ":" + tokens[1];
        } else if (tokens[0].equals(FCR_NAMESPACE_PREFIX) && length == 2 ) {
            return JCR_NAMESPACE_PREFIX + ":" + tokens[1];
        } else {
            return namespace;
        }
    }

    /**
     * Get hierarchy levels
     */
    @Override
    public int getLevels() {
        return levels;
    }
}
