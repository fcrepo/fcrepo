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
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.junit.Assert.assertEquals;

/**
 * @author pwinckles
 */
public class ResourceHeadersAdapterTest {

    @Test
    public void roundTrip() {
        final ResourceHeadersImpl kernelHeaders = new ResourceHeadersImpl();
        kernelHeaders.setDigests(List.of(URI.create("urn:sha-512:blah")));
        kernelHeaders.setStateToken("state");
        kernelHeaders.setParent(FedoraId.getRepositoryRootId());
        kernelHeaders.setId(FedoraId.create("info:fedora/blah"));
        kernelHeaders.setMimeType("text/plain");
        kernelHeaders.setLastModifiedDate(Instant.now());
        kernelHeaders.setLastModifiedBy("modifiedBy");
        kernelHeaders.setInteractionModel(BASIC_CONTAINER.toString());
        kernelHeaders.setObjectRoot(true);
        kernelHeaders.setFilename("filename");
        kernelHeaders.setExternalUrl("externalUrl");
        kernelHeaders.setExternalHandling("externalHandling");
        kernelHeaders.setDeleted(true);
        kernelHeaders.setCreatedDate(Instant.now());
        kernelHeaders.setCreatedBy("createdBy");
        kernelHeaders.setContentSize(100L);
        kernelHeaders.setContentPath("contentPath");
        kernelHeaders.setArchivalGroup(true);

        final ResourceHeaders storageHeaders = new ResourceHeadersAdapter(kernelHeaders).asStorageHeaders();

        final ResourceHeadersImpl roundTrip = new ResourceHeadersAdapter(storageHeaders).asKernelHeaders();

        assertEquals(kernelHeaders.getDigests(), roundTrip.getDigests());
        assertEquals(kernelHeaders.getStateToken(), roundTrip.getStateToken());
        assertEquals(kernelHeaders.getParent(), roundTrip.getParent());
        assertEquals(kernelHeaders.getId(), roundTrip.getId());
        assertEquals(kernelHeaders.getMimeType(), roundTrip.getMimeType());
        assertEquals(kernelHeaders.getLastModifiedBy(), roundTrip.getLastModifiedBy());
        assertEquals(kernelHeaders.getLastModifiedDate(), roundTrip.getLastModifiedDate());
        assertEquals(kernelHeaders.getInteractionModel(), roundTrip.getInteractionModel());
        assertEquals(kernelHeaders.isObjectRoot(), roundTrip.isObjectRoot());
        assertEquals(kernelHeaders.getFilename(), roundTrip.getFilename());
        assertEquals(kernelHeaders.getExternalUrl(), roundTrip.getExternalUrl());
        assertEquals(kernelHeaders.getExternalHandling(), roundTrip.getExternalHandling());
        assertEquals(kernelHeaders.isDeleted(), roundTrip.isDeleted());
        assertEquals(kernelHeaders.getCreatedBy(), roundTrip.getCreatedBy());
        assertEquals(kernelHeaders.getCreatedDate(), roundTrip.getCreatedDate());
        assertEquals(kernelHeaders.getContentSize(), roundTrip.getContentSize());
        assertEquals(kernelHeaders.getContentPath(), roundTrip.getContentPath());
        assertEquals(kernelHeaders.isArchivalGroup(), roundTrip.isArchivalGroup());
    }

}
