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
package org.fcrepo.connector.file;

import static org.fcrepo.jcr.FedoraJcrTypes.CONTENT_DIGEST;
import static org.fcrepo.jcr.FedoraJcrTypes.CONTENT_SIZE;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_CREATED;
import static org.fcrepo.jcr.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.utils.ContentDigest.asURI;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.File;
import java.util.Date;
import java.util.Map;

import org.infinispan.schematic.document.Document;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.federation.spi.DocumentReader;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends the {@link FileSystemConnector} to enable the autocreation of Fedora-specific datastream and
 * content properties.
 *
 * @author Andrew Woods
 *         Date: 1/30/14
 */
public class FedoraFileSystemConnector extends FileSystemConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraFileSystemConnector.class);

    private static final String DELIMITER = "/";
    private static final String JCR_CONTENT = "jcr:content";
    private static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;

    /**
     * This method returns the object/document for the node with the federated arg 'id'.
     * <p/>
     * Additionally, this method adds Fedora datastream and content properties to the result of the parent class
     * implementation.
     */
    @Override
    public Document getDocumentById(final String id) {
        LOGGER.debug("Getting Federated document: {}", id);
        if (null == id || id.isEmpty()) {
            LOGGER.warn("Can not get document with null id");
            return null;
        }

        final Document doc = super.getDocumentById(id);

        final DocumentReader docReader = readDocument(doc);
        final DocumentWriter docWriter = writeDocument(doc);

        final String primaryType = docReader.getPrimaryTypeName();

        if (!docReader.getMixinTypeNames().contains(FEDORA_RESOURCE)) {
            LOGGER.trace("Adding mixin: {}, to {}", FEDORA_RESOURCE, id);
            docWriter.addMixinType(FEDORA_RESOURCE);
        }

        // Is Fedora Datastream?
        if (primaryType.equals(NT_FILE)) {
            decorateDatastreamNode(docReader, docWriter);

        // Is Fedora Content?
        } else if (primaryType.equals(NT_RESOURCE)) {
            decorateContentNode(docReader, docWriter);
        }

        // Persist new properties
        if (!isReadonly()) {
            saveProperties(docReader);
        }

        return docWriter.document();
    }

    @Override
    public String sha1(final File file) {
        final String id = idFor(file) + JCR_CONTENT_SUFFIX;
        final Map<Name, Property> extraProperties = extraPropertiesStore().getProperties(id);
        final Name digestName = nameFrom(CONTENT_DIGEST);
        if (extraProperties.containsKey(digestName)) {
            LOGGER.trace("Found sha1 ({}) for {} in extra properties store.",
                    extraProperties.get(digestName).toString(), file);
            return (extraProperties.get(digestName).toString());
        }
        LOGGER.trace("Computing sha1 for {}.", file);
        return super.sha1(file);
    }

    private boolean hasBeenModifiedSincePropertiesWereStored(final File file, final Property lastModified) {
        if (lastModified == null) {
            return true;
        } else {
            final DateTime datetime = (DateTime) lastModified.getFirstValue();
            return !datetime.toDate().equals(new Date(file.lastModified()));
        }
    }

    private static void decorateDatastreamNode(final DocumentReader docReader, final DocumentWriter docWriter) {
        if (!docReader.getMixinTypeNames().contains(FEDORA_DATASTREAM)) {
            LOGGER.trace("Adding mixin: {}, to {}", FEDORA_DATASTREAM, docReader.getDocumentId());
            docWriter.addMixinType(FEDORA_DATASTREAM);
        }
    }

    private static void decorateContentNode(final DocumentReader docReader, final DocumentWriter docWriter) {
        if (!docReader.getMixinTypeNames().contains(FEDORA_BINARY)) {
            LOGGER.trace("Adding mixin: {}, to {}", FEDORA_BINARY, docReader.getDocumentId());
            docWriter.addMixinType(FEDORA_BINARY);
        }

        if (null == docReader.getProperty(CONTENT_DIGEST)) {
            final BinaryValue binaryValue = getBinaryValue(docReader);
            final String dsChecksum = binaryValue.getHexHash();
            final String dsURI = asURI("SHA-1", dsChecksum).toString();

            LOGGER.trace("Adding {} property of {} to {}", CONTENT_DIGEST, dsURI, docReader.getDocumentId());
            docWriter.addProperty(CONTENT_DIGEST, dsURI);
        }

        if (null == docReader.getProperty(CONTENT_SIZE)) {
            final BinaryValue binaryValue = getBinaryValue(docReader);
            final long binarySize = binaryValue.getSize();

            LOGGER.trace("Adding {} property of {} to {}", CONTENT_SIZE, binarySize, docReader.getDocumentId());
            docWriter.addProperty(CONTENT_SIZE, binarySize);
        }

        LOGGER.debug("Decorated data property at path: {}", docReader.getDocumentId());
    }

    private static BinaryValue getBinaryValue(final DocumentReader docReader) {
        final Property binaryProperty = docReader.getProperty(JCR_DATA);
        return (BinaryValue) binaryProperty.getFirstValue();
    }

    private void saveProperties(final DocumentReader docReader) {
        LOGGER.trace("Persisting properties for {}", docReader.getDocumentId());
        final Map<Name, Property> properties = docReader.getProperties();
        final ExtraProperties extraProperties = extraPropertiesFor(docReader.getDocumentId(), true);
        extraProperties.addAll(properties).except(JCR_PRIMARY_TYPE, JCR_CREATED, JCR_LASTMODIFIED, JCR_DATA);
        extraProperties.save();
    }

}
