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
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.resolveOCFLSubpath;
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
        assertEquals("", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpathWhereRootAndResourceHaveTrailingSlashes() {
        final var rootObjectId = "info:fedora/test/object/";
        final var resourceId = "info:fedora/test/object/resource/";
        assertEquals("resource", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpath() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/resource";
        assertEquals("resource", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpathAcl() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/fcr:acl";
        assertEquals("fcr:acl", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpathNestedAcl() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/nested/fcr:acl";
        assertEquals("nested/fcr:acl", relativizeSubpath(rootObjectId, resourceId));
    }


    @Test
    public void testRelativizeSubpathMetadata() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/fcr:metadata";
        assertEquals("fcr:metadata", relativizeSubpath(rootObjectId, resourceId));
    }

    @Test
    public void testRelativizeSubpathNestedMetadata() {
        final var rootObjectId = "info:fedora/test/object";
        final var resourceId = "info:fedora/test/object/nested/fcr:metadata";
        assertEquals("nested/fcr:metadata", relativizeSubpath(rootObjectId, resourceId));
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

    @Test
    public void testResolveOCFLSubPathRoot(){
        final var rootObjectId = "info:fedora/test/object";
        final var fedoraSubpath = "";
        assertEquals("object", resolveOCFLSubpath(rootObjectId, fedoraSubpath));
    }

    @Test
    public void testResolveOCFLSubPathRootAcl(){
        final var rootObjectId = "info:fedora/test/object";
        final var fedoraSubpath = "fcr:acl";
        assertEquals("object-acl", resolveOCFLSubpath(rootObjectId, fedoraSubpath));
    }

    @Test
    public void testResolveOCFLSubPathRootDescription(){
        final var rootObjectId = "info:fedora/test/object";
        final var fedoraSubpath = "fcr:metadata";
        assertEquals("object-description", resolveOCFLSubpath(rootObjectId, fedoraSubpath));
    }

    @Test
    public void testResolveOCFLSubPathNested(){
        final var rootObjectId = "info:fedora/test/object";
        final var fedoraSubpath = "nested";
        assertEquals("nested", resolveOCFLSubpath(rootObjectId, fedoraSubpath));
    }

    @Test
    public void testResolveOCFLSubPathNestedAcl(){
        final var rootObjectId = "info:fedora/test/object";
        final var fedoraSubpath = "nested/fcr:acl";
        assertEquals("nested-acl", resolveOCFLSubpath(rootObjectId, fedoraSubpath));
    }

    @Test
    public void testResolveOCFLSubPathNestedDescription(){
        final var rootObjectId = "info:fedora/test/object";
        final var fedoraSubpath = "nested/fcr:metadata";
        assertEquals("nested-description", resolveOCFLSubpath(rootObjectId, fedoraSubpath));
    }

}
