/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.connector.file;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Json;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.spi.federation.ExtraPropertiesStore;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

import java.util.Collections;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * An implementation of ExtraPropertyStore, based on
 * org.modeshape.connector.filesystem.JsonSidecarExtraPropertyStore that stores the
 * properties in a separate configured directory than the filesystem federation itself.
 *
 * @author Mike Durbin
 * @author acoburn
 */
public class ExternalJsonSidecarExtraPropertyStore implements ExtraPropertiesStore {

    private FedoraFileSystemConnector connector;

    private File propertyStoreRoot;

    private final DocumentTranslator translator;

    /**
     * Default constructor.
     * @param connector the FileSystemConnector for which this class will store properties.
     * @param propertyStoreRoot the root of a filesystem into which properties will be
     *                          serialized.
     */
    public ExternalJsonSidecarExtraPropertyStore(final FedoraFileSystemConnector connector,
                                                 final DocumentTranslator translator,
                                                 final File propertyStoreRoot) {
        this.connector = connector;
        this.translator = translator;
        this.propertyStoreRoot = propertyStoreRoot;
    }

    protected File sidecarFile(final String id) {
        final File file;
        if (connector.isRoot(id)) {
            file = new File(propertyStoreRoot, "federation-root.modeshape.json");
        } else {
            String ext = ".modeshape.json";
            if (connector.isContentNode(id)) {
                ext = ".content.modeshape.json";
            }
            final File f = new File(connector.fileFor(id).getAbsolutePath() + ext);

            final Path propertyFileInFederation = f.getAbsoluteFile().toPath();
            final Path relativePath = connector.fileFor("/")
                            .getAbsoluteFile().toPath().relativize(propertyFileInFederation);
            file = propertyStoreRoot.toPath().resolve(relativePath).toFile();
        }
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new RepositoryRuntimeException("Unable to create directories " + file.getParentFile() + ".");
        }
        return file;
    }

    /**
     * This is a trivial reimplementation of the private modeshape implementation in
     * org.modeshape.connector.filesystem.JsonSidecarExtraPropertyStore
     *
     * See: https://github.com/ModeShape/modeshape/blob/modeshape-4.2.0.Final/modeshape-jcr/src/main/java/
     *              org/modeshape/connector/filesystem/JsonSidecarExtraPropertyStore.java#L139
     *
     * @param id the identifier for the sidecar file
     * @return whether the file was deleted
     */
    @Override
    public boolean removeProperties(final String id) {
        final File file = sidecarFile(id);
        if (!file.exists()) {
            return false;
        }
        file.delete();
        return true;
    }


    /**
     * This is a trivial reimplementation of the private modeshape implementation in
     * org.modeshape.connector.filesystem.JsonSidecarExtraPropertyStore
     *
     * See: https://github.com/ModeShape/modeshape/blob/modeshape-4.2.0.Final/modeshape-jcr/src/main/java/
     *              org/modeshape/connector/filesystem/JsonSidecarExtraPropertyStore.java#L60
     *
     * @param id the identifier for the sidecar file
     * @return a map of the properties associated with the given configuration
     */
    @Override
    public Map<Name, Property> getProperties(final String id) {
        final File sidecarFile = sidecarFile(id);
        if (!sidecarFile.exists()) {
            return Collections.emptyMap();
        }
        try {
            final Document document = Json.read(new FileInputStream(sidecarFile), false);
            final Map<Name, Property> results = new HashMap<Name, Property>();
            translator.getProperties(document, results);
            return results;
        } catch (IOException e) {
            throw new RepositoryRuntimeException(id, e);
        }
    }

    /**
     * This is a trivial reimplementation of the private modeshape implementation in
     * org.modeshape.connector.filesystem.JsonSidecarExtraPropertyStore
     *
     * See: https://github.com/ModeShape/modeshape/blob/modeshape-4.2.0.Final/modeshape-jcr/src/main/java/
     *              org/modeshape/connector/filesystem/JsonSidecarExtraPropertyStore.java#L74
     *
     * @param id the id for the sidecar file
     * @param properties the keys/values to set in the specified sidecar configuration
     */
    @Override
    public void updateProperties(final String id, final Map<Name, Property> properties ) {
        final File sidecarFile = sidecarFile(id);
        try {
            EditableDocument document = null;
            if (!sidecarFile.exists()) {
                if (properties.isEmpty()) {
                    return;
                }
                sidecarFile.createNewFile();
                document = Schematic.newDocument();
            } else {
                final Document existing = Json.read(new FileInputStream(sidecarFile), false);
                document = Schematic.newDocument(existing);
            }
            for (Map.Entry<Name, Property> entry : properties.entrySet()) {
                final Property property = entry.getValue();
                if (property == null) {
                    translator.removeProperty(document, entry.getKey(), null, null);
                } else {
                    translator.setProperty(document, property, null, null);
                }
            }
            Json.write(document, new FileOutputStream(sidecarFile));
        } catch (IOException e) {
            throw new RepositoryRuntimeException(id, e);
        }
    }

    /**
     * This is a trivial reimplementation of the private modeshape implementation in
     * org.modeshape.connector.filesystem.JsonSidecarExtraPropertyStore
     *
     * See: https://github.com/ModeShape/modeshape/blob/modeshape-4.2.0.Final/modeshape-jcr/src/main/java/
     *              org/modeshape/connector/filesystem/JsonSidecarExtraPropertyStore.java#L102
     *
     * @param id the id for the sidecar file
     * @param properties the keys/values to set in the specified sidecar configuration
     */
    @Override
    public void storeProperties(final String id, final Map<Name, Property> properties ) {
        final File sidecarFile = sidecarFile(id);
        try {
            if (!sidecarFile.exists()) {
                if (properties.isEmpty()) {
                    return;
                }
                sidecarFile.createNewFile();
            }
            final EditableDocument document = Schematic.newDocument();
            for (Property property : properties.values()) {
                if (property == null) {
                    continue;
                }
                translator.setProperty(document, property, null, null);
            }
            Json.write(document, new FileOutputStream(sidecarFile));
        } catch (IOException e) {
            throw new RepositoryRuntimeException(id, e);
        }
    }

    /**
     * This is a trivial reimplementation of the private modeshape implementation in
     * org.modeshape.connector.filesystem.JsonSidecarExtraPropertyStore
     *
     * See: https://github.com/ModeShape/modeshape/blob/modeshape-4.2.0.Final/modeshape-jcr/src/main/java/
     *              org/modeshape/connector/filesystem/JsonSidecarExtraPropertyStore.java#L156
     *
     * @param id the id for the sidecar file
     * @return whether the specified sidecar configuration exists
     */
    @Override
    public boolean contains(final String id) {
        return sidecarFile(id).exists();
    }

}
