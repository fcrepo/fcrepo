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

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author pwinckles
 * @since 6.0.0
 */
public class PersistencePathsTest {

    private static final String FCREPO_DIR = ".fcrepo/";
    private static final String ROOT_PREFIX = "fcr-root";
    private static final String CONTAINER_PREFIX = "fcr-container";
    private static final String ACL_SUFFIX = "~fcr-acl";
    private static final String DESC_SUFFIX = "~fcr-desc";
    private static final String JSON = ".json";
    private static final String NT = ".nt";

    @Test
    public void createRootResourceHeaderPath() {
        final var id = FedoraId.create("binary");
        assertEquals(FCREPO_DIR + ROOT_PREFIX + JSON, PersistencePaths.headerPath(id, id));
    }

    @Test
    public void createAgResourceHeaderPath() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag/sub/path");
        assertEquals(FCREPO_DIR + "sub/path.json", PersistencePaths.headerPath(rootId, id));
    }

    @Test
    public void createAgAclResourceHeaderPath() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag/sub/path").asAcl();
        assertEquals(FCREPO_DIR + "sub/path" + ACL_SUFFIX + JSON, PersistencePaths.headerPath(rootId, id));
    }

    @Test
    public void createAgDescriptionResourceHeaderPath() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag/sub/path").asDescription();
        assertEquals(FCREPO_DIR + "sub/path" + DESC_SUFFIX + JSON, PersistencePaths.headerPath(rootId, id));
    }

    @Test
    public void createRootAclResourceHeaderPath() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag").asAcl();
        assertEquals(FCREPO_DIR + ROOT_PREFIX + ACL_SUFFIX + JSON, PersistencePaths.headerPath(rootId, id));
    }

    @Test
    public void createRootDescriptionResourceHeaderPath() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag").asDescription();
        assertEquals(FCREPO_DIR + ROOT_PREFIX + DESC_SUFFIX + JSON, PersistencePaths.headerPath(rootId, id));
    }

    @Test
    public void createContentPathForAtomicBinary() {
        final var rootId = FedoraId.create("binary");
        final var id = FedoraId.create("binary");
        assertEquals("binary", PersistencePaths.nonRdfContentPath(rootId, id));
    }

    @Test
    public void createContentPathForAgBinary() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag/sub/binary");
        assertEquals("sub/binary", PersistencePaths.nonRdfContentPath(rootId, id));
    }

    @Test
    public void createContentPathForAtomicContainer() {
        final var rootId = FedoraId.create("object");
        final var id = FedoraId.create("object");
        assertEquals(CONTAINER_PREFIX + NT, PersistencePaths.rdfContentPath(rootId, id));
    }

    @Test
    public void createContentPathForAgContainer() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag/foo/bar");
        assertEquals("foo/bar/" + CONTAINER_PREFIX + NT, PersistencePaths.rdfContentPath(rootId, id));
    }

    @Test
    public void createContentPathForAtomicBinaryDesc() {
        final var rootId = FedoraId.create("binary");
        final var id = FedoraId.create("binary").asDescription();
        assertEquals("binary" + DESC_SUFFIX + NT, PersistencePaths.rdfContentPath(rootId, id));
    }

    @Test
    public void createContentPathForAgBinaryDesc() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag/sub/binary").asDescription();
        assertEquals("sub/binary" + DESC_SUFFIX + NT, PersistencePaths.rdfContentPath(rootId, id));
    }

    @Test
    public void createContentPathForAtomicBinaryAcl() {
        final var rootId = FedoraId.create("binary");
        final var id = FedoraId.create("binary").asAcl();
        assertEquals("binary" + ACL_SUFFIX + NT, PersistencePaths.aclContentPath(false, rootId, id));
    }

    @Test
    public void createContentPathForAgBinaryAcl() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag/sub/binary").asAcl();
        assertEquals("sub/binary" + ACL_SUFFIX + NT, PersistencePaths.aclContentPath(false, rootId, id));
    }

    @Test
    public void createContentPathForAtomicContainerAcl() {
        final var rootId = FedoraId.create("object");
        final var id = FedoraId.create("object").asAcl();
        assertEquals(CONTAINER_PREFIX + ACL_SUFFIX + NT, PersistencePaths.aclContentPath(true, rootId, id));
    }

    @Test
    public void createContentPathForAgContainerAcl() {
        final var rootId = FedoraId.create("ag");
        final var id = FedoraId.create("ag/foo/bar").asAcl();
        assertEquals("foo/bar/" + CONTAINER_PREFIX + ACL_SUFFIX + NT,
                PersistencePaths.aclContentPath(true, rootId, id));
    }

    @Test
    public void trueWhenPathInFcrepo() {
        assertTrue(PersistencePaths.isHeaderFile(FCREPO_DIR + "blah.json"));
    }

    @Test
    public void falseWhenPathNotInFcrepo() {
        assertFalse(PersistencePaths.isHeaderFile("blah.json"));
    }

}
