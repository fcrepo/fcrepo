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
package org.fcrepo.kernel.api.utils;

import java.time.Instant;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.services.VersionService;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FedoraResourceIdConverterTest {

    private final String id = "fedora:info/test-id";
    private final String fcrVersions = "/" + FedoraTypes.FCR_VERSIONS;

    @Test
    public void doNotChangeIdWhenNotATimeMap() {
        final var resolvedId = FedoraResourceIdConverter.resolveFedoraId(resource(id, Container.class));
        assertEquals(id, resolvedId);
    }

    @Test
    public void changeIdWhenTimeMap() {
        final var resolvedId = FedoraResourceIdConverter.resolveFedoraId(resource(id, TimeMap.class));
        assertEquals(id + fcrVersions, resolvedId);
    }

    @Test
    public void changeIdWhenMemento() {
        final var timestamp = "20200226183405";
        final var now = Instant.from(VersionService.MEMENTO_LABEL_FORMATTER.parse(timestamp));
        final var resource = memento(id, now, Binary.class);

        final var resolvedId = FedoraResourceIdConverter.resolveFedoraId(resource);
        assertEquals(id + fcrVersions + "/" + timestamp, resolvedId);
    }

    private <T extends FedoraResource> T memento(final String resourceId, final Instant instant, final Class<T> clazz) {
        final var mock = mock(clazz);
        when(mock.getId()).thenReturn(resourceId);
        when(mock.isMemento()).thenReturn(true);
        when(mock.getMementoDatetime()).thenReturn(instant);
        return mock;
    }

    private <T extends FedoraResource> T resource(final String resourceId, final Class<T> clazz) {
        final var mock = mock(clazz);
        when(mock.getId()).thenReturn(resourceId);
        when(mock.isMemento()).thenReturn(false);
        return mock;
    }

}
