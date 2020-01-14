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

import static org.apache.commons.lang3.SystemUtils.JAVA_IO_TMPDIR;

import static java.lang.System.getProperty;

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
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private Path indexFilePath = getProperty("fcrepo.ocfl.work.dir") == null ?
            Paths.get(JAVA_IO_TMPDIR, "fcrepo.ocfl.work.dir", "fedoraToOcflIndex.tsv") :
            Paths.get(getProperty("fcrepo.ocfl.work.dir"), "fedoraToOcflIndex.tsv");

    private File indexFile = indexFilePath.toFile();

    private static Logger LOGGER = LoggerFactory.getLogger(FedoraToOCFLObjectIndexImpl.class);

    private Map<String, FedoraOCFLMapping> fedoraOCFLMappingMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructor
     * @throws IOException If we can't access the file.
     */
    public FedoraToOCFLObjectIndexImpl() throws IOException {
        if (indexFile.exists() && indexFile.canRead()) {
            Files.lines(indexFile.toPath()).filter(l -> {
                final String m = l.split("\t")[0];
                return (!fedoraOCFLMappingMap.containsKey(m));
            }).forEach(l -> {
                final String[] map = l.split("\t");
                if (map.length == 3) {
                    final FedoraOCFLMapping ocflMapping = new FedoraOCFLMapping(map[1], map[2]);
                    fedoraOCFLMappingMap.put(map[0], ocflMapping);
                }
            });
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
    public FedoraOCFLMapping addMapping(final String fedoraResourceIdentifier, final String fedoraRootObjectResourceId, final String ocflObjectId) {
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
            if (!indexFile.exists()) {
                final String dirName = indexFile.toPath().toAbsolutePath().toString().substring(0,
                        indexFile.toPath().toAbsolutePath().toString().lastIndexOf(File.separator));
                final File dir = new File(dirName);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                indexFile.createNewFile();
            }
            final BufferedWriter output = new BufferedWriter(new FileWriter(indexFile, true));
            output.write(String.format("%s\t%s\t%s\n", fedoraId, fedoraOCFLMapping.getRootObjectIdentifier(),
                    fedoraOCFLMapping.getOcflObjectId()));
            output.close();
        } catch (IOException exception) {
            LOGGER.warn("Unable to create/write on disk FedoraToOCFL Mapping at {}", indexFile.toString());
        }
    }
}
