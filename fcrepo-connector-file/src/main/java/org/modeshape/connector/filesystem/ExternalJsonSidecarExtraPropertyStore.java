/**
 * Copyright 2014 DuraSpace, Inc.
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

package org.modeshape.connector.filesystem;

import org.modeshape.jcr.cache.document.DocumentTranslator;

import java.io.File;

/**
 * An extension of JsonSidecarExtraPropertyStore that stores the properties in a
 * separate configured directory than the filesystem federation itself.  Because
 * the class we're extending is package protected this class is in the
 * org.modeshape.connector.filesystem package.
 *
 * @author Mike Durbin
 */
public class ExternalJsonSidecarExtraPropertyStore extends JsonSidecarExtraPropertyStore {

    private File propertyStoreRoot;

    private FileSystemConnector connector;

    /**
     * Default constructor.
     * @param connector the FileSystemConnector for which this class will store properties.
     * @param propertyStoreRoot the root of a filesystem into which properties will be
     *                          serialized.
     */
    public ExternalJsonSidecarExtraPropertyStore(final FileSystemConnector connector,
                                                 final DocumentTranslator translator,
                                                 final File propertyStoreRoot) {
        super(connector, translator);
        this.connector = connector;
        this.propertyStoreRoot = propertyStoreRoot;
    }

    @Override
    protected File sidecarFile(final String id) {
        final File propertyFileInFederation = super.sidecarFile(id);
        final String relativePath
                = propertyFileInFederation.getPath().substring(connector.fileFor("/").getPath().length());
        final File file = new File(propertyStoreRoot,
                connector.isRoot(id) ? "federation-root.modeshape.json" : relativePath);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new RuntimeException("Unable to crate directories " + file.getParentFile() + ".");
        }
        return file;
    }
}
