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

package org.fcrepo.kernel.rdf;

public class HierarchyRdfContextOptions {
    public static final HierarchyRdfContextOptions DEFAULT = HierarchyRdfContextOptions.defaultOptions();

    public int limit;
    public int offset;
    public boolean membership;
    public boolean containment;

    /**
     * Options with the default values
     */
    public HierarchyRdfContextOptions() {
        this.limit = -1;
        this.offset = -1;
        membership = true;
        containment = true;
    }

    /**
     * Options with a limit and offset
     * @param limit
     * @param offset
     */
    public HierarchyRdfContextOptions(int limit, int offset) {
        this();
        this.limit = limit;
        this.offset = offset;
    }


    /**
     * Should the serialization include membership triples
     * @return
     */
    public boolean membershipEnabled() {
        return membership || containment;
    }

    /**
     * Should the serialziation include containment triples
     * @return
     */
    public boolean containmentEnabled() {
        return containment;
    }

    /**
     * Is there a pagination offset that needs to be applied>?
     * @return
     */
    public boolean hasOffset() {
        return getOffset() >= 0;
    }

    /**
     * Is there a pagination limit that needs to be applied?
     * @return
     */
    public boolean hasLimit() {
        return getLimit() >= 0;
    }

    /**
     * Get the pagination limit value
     * @return
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Get the pagination offset value
     * @return
     */
    public int getOffset() {
        return offset;
    }

    private static HierarchyRdfContextOptions defaultOptions() {
        HierarchyRdfContextOptions options = new HierarchyRdfContextOptions();

        return options;
    }

}
