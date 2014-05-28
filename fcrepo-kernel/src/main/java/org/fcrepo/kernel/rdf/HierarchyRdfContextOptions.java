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
package org.fcrepo.kernel.rdf;

/**
 * Options for the HierarchyRdfContext serialization
 * @author cabeer
 */
public class HierarchyRdfContextOptions {
    public static final HierarchyRdfContextOptions DEFAULT = HierarchyRdfContextOptions.defaultOptions();
    public static final int DEFAULT_LIMIT = -1;
    public static final int DEFAULT_OFFSET = -1;
    public static final boolean DEFAULT_MEMBERSHIP = true;
    public static final boolean DEFAULT_CONTAINMENT = true;

    final private int limit;
    final private int offset;
    final private boolean membership;
    final private boolean containment;

    /**
     * Options with the default values
     */
    public HierarchyRdfContextOptions() {
        this(DEFAULT_LIMIT, DEFAULT_OFFSET);
    }

    /**
     * Options with a limit and offset
     * @param limit
     * @param offset
     */
    public HierarchyRdfContextOptions(final int limit, final int offset) {
        this(limit, offset, DEFAULT_MEMBERSHIP, DEFAULT_CONTAINMENT);
    }

    /**
     * Set the full range of hierarchy options
     * @param limit number of hierarchy nodes to display
     * @param offset pagination offset
     * @param membership include membership triples
     * @param containment include containment triples
     */
    public HierarchyRdfContextOptions(final int limit,
                                      final int offset,
                                      final boolean membership,
                                      final boolean containment) {
        this.limit = limit;
        this.offset = offset;
        this.membership = membership;
        this.containment = containment;
    }


    /**
     * Should the serialization include membership triples
     * @return serialization should include membership triples
     */
    public boolean membershipEnabled() {
        return membership || containment;
    }

    /**
     * Should the serialziation include containment triples
     * @return should include containment triples
     */
    public boolean containmentEnabled() {
        return containment;
    }

    /**
     * Is there a pagination offset that needs to be applied?
     * @return boolean
     */
    public boolean hasOffset() {
        return getOffset() >= 0;
    }

    /**
     * Is there a pagination limit that needs to be applied?
     * @return boolean
     */
    public boolean hasLimit() {
        return getLimit() >= 0;
    }

    /**
     * Get the pagination limit value
     * @return pagination limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Get the pagination offset value
     * @return pagination offset
     */
    public int getOffset() {
        return offset;
    }

    private static HierarchyRdfContextOptions defaultOptions() {
        return new HierarchyRdfContextOptions();
    }

}
