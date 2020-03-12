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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public class FedoraToOCFLObjectIndexImplTest {

    private static final String RESOURCE_ID_1 = "info:fedora/parent/child1";
    private static final String RESOURCE_ID_2 = "info:fedora/parent/child2";
    private static final String RESOURCE_ID_3 = "info:fedora/resource3";
    private static final String ROOT_RESOURCE_ID = "info:fedora/parent";
    private static final String OCFL_ID = "ocfl-id";
    private static final String OCFL_ID_RESOURCE_3 = "ocfl-id-resource-3";

    private File fedoraToOcflIndexFile;

    @Before
    public void setup() {
        fedoraToOcflIndexFile = new OCFLConstants().getFedoraToOCFLIndexFile();
        removeIndexMappingFile();
    }

    @Test
    public void test() throws Exception {
        final FedoraToOCFLObjectIndexImpl index = new FedoraToOCFLObjectIndexImpl();

        index.addMapping(RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(RESOURCE_ID_2, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(RESOURCE_ID_3, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        final FedoraOCFLMapping mapping1 = index.getMapping(RESOURCE_ID_1);
        final FedoraOCFLMapping mapping2 = index.getMapping(RESOURCE_ID_2);
        final FedoraOCFLMapping mapping3 = index.getMapping(ROOT_RESOURCE_ID);

        assertEquals(mapping1, mapping2);
        assertEquals(mapping2, mapping3);

        verifyMapping(mapping1, ROOT_RESOURCE_ID, OCFL_ID);

        final FedoraOCFLMapping mapping4 = index.getMapping(RESOURCE_ID_3);
        assertNotEquals(mapping4, mapping3);

        verifyMapping(mapping4, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        assertEquals(ROOT_RESOURCE_ID, mapping1.getRootObjectIdentifier());
        assertEquals(OCFL_ID, mapping1.getOcflObjectId());
    }

    private void verifyMapping(final FedoraOCFLMapping mapping1, final String rootResourceId, final String ocflId) {
        assertEquals(rootResourceId, mapping1.getRootObjectIdentifier());
        assertEquals(ocflId, mapping1.getOcflObjectId());
    }

    @Test(expected = FedoraOCFLMappingNotFoundException.class)
    public void testNotExists() throws Exception {
        final FedoraToOCFLObjectIndexImpl index = new FedoraToOCFLObjectIndexImpl();
        index.getMapping(RESOURCE_ID_1);
    }

    @Test
    public void testSaveToIndex() throws Exception {
        assertFalse(fedoraToOcflIndexFile.exists());

        final FedoraToOCFLObjectIndexImpl index = new FedoraToOCFLObjectIndexImpl();

        index.addMapping(RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(RESOURCE_ID_2, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(RESOURCE_ID_3, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        assertTrue(fedoraToOcflIndexFile.exists());

        final List<String> lines;
        try (var l = Files.lines(fedoraToOcflIndexFile.toPath())) {
            lines = l.collect(Collectors.toList());
        }

        assertEquals(4, lines.size());
    }

    @Test
    public void testReadFromIndex() throws Exception {
        assertFalse(fedoraToOcflIndexFile.exists());

        final BufferedWriter output = new BufferedWriter(new FileWriter(fedoraToOcflIndexFile, true));
        output.write(String.format("%s\t%s\t%s\n", RESOURCE_ID_2, ROOT_RESOURCE_ID, OCFL_ID));
        output.close();

        assertTrue(fedoraToOcflIndexFile.exists());

        final FedoraToOCFLObjectIndexImpl index = new FedoraToOCFLObjectIndexImpl();
        try {
            index.getMapping(RESOURCE_ID_1);
            fail();
        } catch (FedoraOCFLMappingNotFoundException e) {
            // We're okay
        }

        final FedoraOCFLMapping mapping = index.getMapping(RESOURCE_ID_2);
        assertEquals(ROOT_RESOURCE_ID, mapping.getRootObjectIdentifier());
        assertEquals(OCFL_ID, mapping.getOcflObjectId());

        try {
            index.getMapping(RESOURCE_ID_3);
            fail();
        } catch (FedoraOCFLMappingNotFoundException e) {
            // We're okay
        }
    }

    private void removeIndexMappingFile() {
        if (fedoraToOcflIndexFile.exists() &&
                !fedoraToOcflIndexFile.delete()) {
            throw new RuntimeException("Could not delete file " + fedoraToOcflIndexFile);
        }
    }
}