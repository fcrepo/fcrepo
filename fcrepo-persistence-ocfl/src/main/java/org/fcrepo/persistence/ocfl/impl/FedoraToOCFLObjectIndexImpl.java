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

import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An simple in-memory implementation of the {@link org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex}
 *
 * @author dbernstein
 * @since 6.0.0
 */
@Component
public class FedoraToOCFLObjectIndexImpl implements FedoraToOCFLObjectIndex {

    private static Logger LOGGER = LoggerFactory.getLogger(FedoraToOCFLObjectIndexImpl.class);

    private Map<String, FedoraOCFLMapping> fedoraOCFLMappingMap = Collections.synchronizedMap(new HashMap<>());

    private final File fedoraToOcflIndexFile;

    /**
     * Constructor.
     *
     * On disk index file is a text file with 3 values per line separated by tabs.
     * 1. fedora identifier (ie. info:fedora/parent/object1 or info:fedora/object1)
     * 2. fedora root object ID (ie. info:fedora/parent or info:fedora/object1). Used for root of Archival groups.
     * 3. OCFL object ID (ie. parent/object1 or object1)
     *
     * @throws IOException If we can't access the file.
     */
    public FedoraToOCFLObjectIndexImpl() throws IOException {
        fedoraToOcflIndexFile = new OCFLConstants().getFedoraToOCFLIndexFile();
        if (fedoraToOcflIndexFile.exists() && fedoraToOcflIndexFile.canRead()) {
            try (var lines = Files.lines(fedoraToOcflIndexFile.toPath())) {
               lines.filter(l -> {
                    final String m = l.split("\t")[0];
                    return (!fedoraOCFLMappingMap.containsKey(m));
                }).forEach(l -> {
                    final String[] map = l.split("\t");
                    if (map.length == 3) {
                        final FedoraOCFLMapping ocflMapping = new FedoraOCFLMapping(map[1], map[2]);
                        fedoraOCFLMappingMap.put(map[0], ocflMapping);
                    } else {
                        LOGGER.warn("Expected 3 tab-separated values, found {}. Ignoring line.", map.length);
                    }
                });
            }
        }
    }

    @Override
    public FedoraOCFLMapping getMapping(final String fedoraResourceIdentifier)
            throws FedoraOCFLMappingNotFoundException {

        LOGGER.debug("getting {}", fedoraResourceIdentifier);
        final FedoraOCFLMapping m = fedoraOCFLMappingMap.get(fedoraResourceIdentifier);
        if (m == null) {
            throw new FedoraOCFLMappingNotFoundException(fedoraResourceIdentifier);
        }

        return m;
    }

    @Override
    public FedoraOCFLMapping addMapping(final String fedoraResourceIdentifier,
                                        final String fedoraRootObjectResourceId,
                                        final String ocflObjectId) {
        FedoraOCFLMapping mapping = fedoraOCFLMappingMap.get(fedoraRootObjectResourceId);

        if (mapping == null) {
            mapping = new FedoraOCFLMapping(fedoraRootObjectResourceId, ocflObjectId);
            fedoraOCFLMappingMap.put(fedoraRootObjectResourceId, mapping);
            writeMappingToDisk(fedoraRootObjectResourceId, mapping);
        }

        if (!fedoraResourceIdentifier.equals(fedoraRootObjectResourceId)) {
            fedoraOCFLMappingMap.put(fedoraResourceIdentifier, mapping);
            writeMappingToDisk(fedoraResourceIdentifier, mapping);
        }

        LOGGER.debug("added mapping {} for {}", mapping, fedoraResourceIdentifier);
        return mapping;
    }

    /**
     * Write any added mappings to the on-disk index.
     * @param fedoraId The internal Fedora identifier.
     * @param fedoraOCFLMapping The Fedora to OCFL mapping object.
     */
    private void writeMappingToDisk(final String fedoraId, final FedoraOCFLMapping fedoraOCFLMapping) {
        try {
            if (!fedoraToOcflIndexFile.exists()) {
                final File dir = fedoraToOcflIndexFile.getParentFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            }
            try (var fw = new FileWriter(fedoraToOcflIndexFile, true); var output= new BufferedWriter(fw)) {
                output.write(String.format("%s\t%s\t%s\n", fedoraId, fedoraOCFLMapping.getRootObjectIdentifier(),
                        fedoraOCFLMapping.getOcflObjectId()));
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to create/write on disk FedoraToOCFL Mapping at {}", fedoraToOcflIndexFile);
        }
    }

    @Override
    public void reset() {
        fedoraOCFLMappingMap.clear();
        if (fedoraToOcflIndexFile.exists()) {
            fedoraToOcflIndexFile.delete();
        }
    }
}
