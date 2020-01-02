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

import static org.junit.Assert.assertEquals;

import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.junit.Test;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public class FedoraToOCFLObjectIndexImplTest {

    private static final String RESOURCE_ID_1 = "info:fedora/parent/child1";
    private static final String RESOURCE_ID_2 = "info:fedora/parent/child1";

    private static final String ROOT_RESOURCE_ID = "info:fedora/parent";
    private static final String OCFL_ID = "ocfl-id";

    @Test
    public void test() throws Exception {
        final FedoraToOCFLObjectIndexImpl index = new FedoraToOCFLObjectIndexImpl();

        index.addMapping(RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(RESOURCE_ID_2, ROOT_RESOURCE_ID, OCFL_ID);

        final FedoraOCFLMapping mapping1 = index.getMapping(RESOURCE_ID_1);
        final FedoraOCFLMapping mapping2 = index.getMapping(RESOURCE_ID_2);
        final FedoraOCFLMapping mapping3 = index.getMapping(ROOT_RESOURCE_ID);

        assertEquals(mapping1, mapping2);
        assertEquals(mapping2, mapping3);
        assertEquals(ROOT_RESOURCE_ID, mapping1.getRootObjectIdentifier());
        assertEquals(OCFL_ID, mapping1.getOcflObjectId());
    }

    @Test(expected = FedoraOCFLMappingNotFoundException.class)
    public void testNotExists() throws Exception {
        final FedoraToOCFLObjectIndexImpl index = new FedoraToOCFLObjectIndexImpl();
        index.getMapping(RESOURCE_ID_1);
    }
}