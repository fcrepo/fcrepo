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
package org.fcrepo.kernel.impl.utils.impl;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.Property;

import org.modeshape.jcr.value.binary.FileSystemBinaryStore;

/**
 * @author cabeer
 */
public class FileSystemBinaryStoreEntry extends LocalBinaryStoreEntry {

    /**
     * Create a binary store entry for a property in a filesystem binary store
     * @param store
     * @param property
     */
    public FileSystemBinaryStoreEntry(final FileSystemBinaryStore store, final Property property) {
        super(store, property);
    }

    @Override
    public String getExternalIdentifier() {
        try {
            final FileSystemBinaryStore store = (FileSystemBinaryStore)store();
            return new URI("info",
                              store.toString(),
                              store.getDirectory().getAbsolutePath(),
                              binaryKey().toString()).toString();
        } catch (URISyntaxException e) {
            return binaryKey().toString();
        }
    }

}
