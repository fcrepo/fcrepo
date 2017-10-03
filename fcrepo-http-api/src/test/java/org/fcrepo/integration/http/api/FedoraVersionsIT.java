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
package org.fcrepo.integration.http.api;

import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;

import java.io.IOException;

import org.junit.Test;

/**
 * <p>
 * FedoraVersionsIT class.
 * </p>
 *
 * @author awoods
 * @author ajs6f
 */
public class FedoraVersionsIT extends AbstractResourceIT {

    private static final String[] jcrVersioningTriples = new String[] {
        REPOSITORY_NAMESPACE + "baseVersion",
        REPOSITORY_NAMESPACE + "isCheckedOut",
        REPOSITORY_NAMESPACE + "predecessors",
        REPOSITORY_NAMESPACE + "versionHistory" };

    @Test
    public void testGetObjectVersionProfile() throws IOException {
    }

    @Test
    public void testGetUnversionedObjectVersionProfile() {
    }

    @Test
    public void testAddAndRetrieveVersion() throws IOException {
    }

    @Test
    public void testVersioningANodeWithAVersionableChild() throws IOException {
    }

    @Test
    public void testVersionHeaders() throws IOException {
    }

    @Test
    public void testVersionListHeaders() throws IOException {
    }

    @Test
    public void testGetDatastreamVersionNotFound() throws Exception {
    }

    public void mutateDatastream(final String objName, final String dsName, final String contentText) {
    }

    @Test
    public void testInvalidVersionReversion() {
    }

    @Test
    public void testVersionReversion() throws IOException {
    }

    @Test
    public void testRemoveVersion() throws IOException {
    }

    @Test
    public void testRemoveInvalidVersion() {
    }

    @Test
    public void testRemoveCurrentVersion() throws IOException {
    }

    @Test
    public void testVersionOperationAddsVersionableMixin() throws IOException {
    }

    @Test
    public void testIndexResponseContentTypes() throws IOException {
    }

    @Test
    public void testGetVersionResponseContentTypes() throws IOException {
    }

    @Test
    public void testOmissionOfJCRCVersionRDF() throws IOException {
    }

    @Test
    public void testFixityOnVersionedResource() throws IOException {
    }
}
