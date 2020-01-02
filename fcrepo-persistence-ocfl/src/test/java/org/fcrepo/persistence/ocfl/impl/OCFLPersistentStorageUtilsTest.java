/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.junit.Test;

import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.relativizeSubpath;
import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link OCFLPersistentStorageUtils}
 *
 * @author dbernstein
 */
public class OCFLPersistentStorageUtilsTest {

    @Test
    public void testRelativizeSubpathWhereRootEqualsResource() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object";
        assertEquals("object", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpathWhereRootAndResourceHaveTrailingSlashes() {
        final var rootObjectId = "info:fedora/test/object/";
        final var resourceId = "info:fedora/test/object/resource/";
        assertEquals("object/resource", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpath() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/resource";
        assertEquals("object/resource", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpathAcl() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/fcr:acl";
        assertEquals("object-acl", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpathNestedAcl() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/nested/fcr:acl";
        assertEquals("object/nested-acl", relativizeSubpath(rootObjectId, resourceId));
    }


    @Test
    public void testRelativizeSubpathMetadata() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/fcr:metadata";
        assertEquals("object-description", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpathNestedMetadata() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/nested/fcr:metadata";
        assertEquals("object/nested-description", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResourceNotPrefixedByRoot1() {
        final var rootObjectId = "info:fedora/test/a";
        final var resourceId = "info:fedora/test/b";
        relativizeSubpath(rootObjectId, resourceId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResourceNotPrefixedByRoot2() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object2";
        relativizeSubpath(rootObjectId, resourceId);
    }

}
