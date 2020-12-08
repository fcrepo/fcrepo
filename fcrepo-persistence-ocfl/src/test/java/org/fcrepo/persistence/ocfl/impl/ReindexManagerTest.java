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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Reindex manager tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReindexManagerTest extends AbstractReindexerTest {

    @Mock
    private ReindexService reindexService;

    private ReindexManager reindexManager;

    @Before
    public void setup() throws Exception {
        super.setup();
        reindexManager = new ReindexManager(repository.listObjectIds(), reindexService, propsConfig);
    }

    @Test
    public void testProcessAnObject() throws Exception {
        final var session = persistentStorageSessionManager.getSession(session1Id);

        createResource(session, resource1, true);
        createChildResourceNonRdf(session, resource1, resource2);

        session.prepare();
        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        ocflIndex.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        reindexManager.start();
        reindexManager.commit();
        reindexManager.shutdown();

        verify(reindexService).indexOcflObject(anyString(), eq(FEDORA_ID_PREFIX + "/resource1"));
        verify(reindexService).commit(anyString());
        verify(reindexService).indexMembership(anyString());
    }
}
