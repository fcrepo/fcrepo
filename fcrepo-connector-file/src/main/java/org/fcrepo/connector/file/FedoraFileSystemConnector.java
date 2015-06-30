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

import static java.lang.System.currentTimeMillis;
import static org.fcrepo.kernel.FedoraJcrTypes.CONTENT_DIGEST;
import static org.fcrepo.kernel.FedoraJcrTypes.CONTENT_SIZE;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.FedoraJcrTypes.JCR_CREATED;
import static org.fcrepo.kernel.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.utils.ContentDigest.asURI;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.io.IOException;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.infinispan.schematic.document.Document;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.spi.federation.DocumentChanges;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.basic.BasicSingleValueProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

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
     * The string path for a {@link File} object that represents the top-level directory in which properties are
     * stored.  This is optional for this connector, but if set allows properties to be cached (greatly
     * improving performance) for even read-only connectors.  When this property is specified the extraPropertiesStore
     * should be null (not specified) as it would be overridden by this.
     */
    private String propertiesDirectoryPath;
    private File propertiesDirectory;

    @Override
    public void initialize(final NamespaceRegistry registry,
                           final NodeTypeManager nodeTypeManager) throws IOException {
        try {
            super.initialize(registry, nodeTypeManager);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException("Error initializing FedoraFileSystemConnector!", e);
        }

        if (propertiesDirectoryPath != null) {
           propertiesDirectory = new File(propertiesDirectoryPath);
            if (!propertiesDirectory.exists() || !propertiesDirectory.isDirectory()) {
                throw new RepositoryRuntimeException("Configured \"propertiesDirectory\", " + propertiesDirectoryPath
                        + ", does not exist or is not a directory.");
            } else if ( !propertiesDirectory.canRead() || !propertiesDirectory.canWrite() ) {
                throw new RepositoryRuntimeException("Configured \"propertiesDirectory\", " + propertiesDirectoryPath
                        + ", should be readable and writable.");
            }
            if (extraPropertiesStore() != null) {
                LOGGER.warn("Extra properties store was specified but won't be used!");
            }
            setExtraPropertiesStore(new ExternalJsonSidecarExtraPropertyStore(this, translator(), propertiesDirectory));
        }
    }

    /**
     * This method returns the object/document for the node with the federated arg 'id'.
     *
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
        if ( doc == null ) {
            LOGGER.debug("Non-existent node, document is null: {}", id);
            return doc;
        }

        final DocumentReader docReader = readDocument(doc);
        final DocumentWriter docWriter = writeDocument(doc);
        final long lastmod = fileFor(id).lastModified();
        LOGGER.debug("Adding lastModified={}", lastmod);
        docWriter.addProperty(JCR_LASTMODIFIED, lastmod);

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
            decorateContentNode(docReader, docWriter, fileFor(id));

        // Is Fedora Object?
        } else if (primaryType.equals(NT_FOLDER)) {
            decorateObjectNode(docReader, docWriter);
        }

        return docWriter.document();
    }

    /**
     * Checks whether internally managed properties can and should be stored to
     * an ExtraPropertiesStore.
     * @return whether internally managed properties can and should be stored to
     */
    protected boolean shouldCacheProperties() {
        return extraPropertiesStore() != null && (!isReadonly() || this.propertiesDirectory != null);
    }


    /**
     * Pass-thru to the parent class in order to make this function public
     *
     * @param id the node ID to test
     * @return whether the id corresponds to the root location
     */
    @Override
    public boolean isRoot(final String id) {
        return super.isRoot(id);
    }

    /**
     * Pass-thru to the parent class in order to make this function public
     *
     * @param file the file used to compute a sha1 hash
     * @return the sha1 hash of the file contents
     */
    @Override
    public String sha1(final File file) {
        final String cachedSha1 = getCachedSha1(file);
        if (cachedSha1 == null) {
            return computeAndCacheSha1(file);
        }
        return cachedSha1;
    }

    private String getCachedSha1(final File file) {
        final String id = idFor(file) + JCR_CONTENT_SUFFIX;
        if (extraPropertiesStore() != null) {
            final Map<Name, Property> extraProperties = extraPropertiesStore().getProperties(id);
            final Name digestName = nameFrom(CONTENT_DIGEST);
            if (extraProperties.containsKey(digestName)) {
                if (!hasBeenModifiedSincePropertiesWereStored(file, extraProperties.get(nameFrom(JCR_CREATED)))) {
                    LOGGER.trace("Found sha1 for {} in extra properties store.", id);
                    final String uriStr = ((URI) extraProperties.get(digestName).getFirstValue()).toString();
                    return uriStr.substring(uriStr.indexOf("sha1:") + 5);
                }
            }
        } else {
            LOGGER.trace("No cache configured to contain object hashes.");
        }
        return null;
    }

    private String computeAndCacheSha1(final File file) {
        final String id = idFor(file) + JCR_CONTENT_SUFFIX;
        LOGGER.trace("Computing sha1 for {}.", id);
        final String sha1 = super.sha1(file);
        if (shouldCacheProperties()) {
            final Map<Name, Property> updateMap = new HashMap<>();
            final Property digestProperty = new BasicSingleValueProperty(nameFrom(CONTENT_DIGEST),
                    asURI("SHA-1", sha1));
            final Property digestDateProperty = new BasicSingleValueProperty(nameFrom(JCR_CREATED),
                    factories().getDateFactory().create(file.lastModified()));
            updateMap.put(digestProperty.getName(), digestProperty);
            updateMap.put(digestDateProperty.getName(), digestDateProperty);
            extraPropertiesStore().updateProperties(id, updateMap);
        }
        return sha1;
    }

    private static void decorateObjectNode(final DocumentReader docReader, final DocumentWriter docWriter) {
        if (!docReader.getMixinTypeNames().contains(FEDORA_CONTAINER)) {
            LOGGER.trace("Adding mixin: {}, to {}", FEDORA_CONTAINER, docReader.getDocumentId());
            docWriter.addMixinType(FEDORA_CONTAINER);
        }
    }

    private static void decorateDatastreamNode(final DocumentReader docReader, final DocumentWriter docWriter) {
        if (!docReader.getMixinTypeNames().contains(FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
            LOGGER.trace("Adding mixin: {}, to {}", FEDORA_NON_RDF_SOURCE_DESCRIPTION, docReader.getDocumentId());
            docWriter.addMixinType(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        }
    }

    private static void decorateContentNode(final DocumentReader docReader,
                                            final DocumentWriter docWriter,
                                            final File file) {
        if (!docReader.getMixinTypeNames().contains(FEDORA_BINARY)) {
            LOGGER.trace("Adding mixin: {}, to {}", FEDORA_BINARY, docReader.getDocumentId());
            docWriter.addMixinType(FEDORA_BINARY);
        }

        if (null == docReader.getProperty(CONTENT_DIGEST)
                || hasBeenModifiedSincePropertiesWereStored(file, docReader.getProperty(JCR_CREATED))) {
            final BinaryValue binaryValue = getBinaryValue(docReader);
            final String dsChecksum = binaryValue.getHexHash();
            final String dsURI = asURI("SHA-1", dsChecksum).toString();

            LOGGER.trace("Adding {} property of {} to {}", CONTENT_DIGEST, dsURI, docReader.getDocumentId());
            docWriter.addProperty(CONTENT_DIGEST, dsURI);
        }

        if (null == docReader.getProperty(CONTENT_SIZE)) {
            final long binarySize = file.length();
            LOGGER.trace("Adding {} property of {} to {}", CONTENT_SIZE, binarySize, docReader.getDocumentId());
            docWriter.addProperty(CONTENT_SIZE, binarySize);
        }

        LOGGER.debug("Decorated data property at path: {}", docReader.getDocumentId());
    }

    private static boolean hasBeenModifiedSincePropertiesWereStored(final File file, final Property lastModified) {
        if (lastModified == null) {
            LOGGER.trace("Hash for {} has not been computed yet.", file.getName());
            return true;
        }
        final DateTime datetime = (DateTime) lastModified.getFirstValue();
        if (datetime.toDate().equals(new Date(file.lastModified()))) {
            return false;
        }
        LOGGER.trace("{} has been modified ({}) since hash was last computed ({}).", file.getName(),
                new Date(file.lastModified()), datetime.toDate());
        return true;
    }

    private static BinaryValue getBinaryValue(final DocumentReader docReader) {
        final Property binaryProperty = docReader.getProperty(JCR_DATA);
        return (BinaryValue) binaryProperty.getFirstValue();
    }

    /* Override write operations to also update the parent file's timestamp, so
       its Last-Modified header correctly reflects changes to children. */
    @Override
    public boolean removeDocument( final String id ) {
        if ( super.removeDocument(id) ) {
            touchParent(id);
            return true;
        }
        return false;
    }

    @Override
    public void storeDocument( final Document document ) {
        super.storeDocument( document );
        touchParent(readDocument(document).getDocumentId());
    }

    @Override
    public void updateDocument( final DocumentChanges changes ) {
        super.updateDocument( changes );
        touchParent( changes.getDocumentId() );
    }

    /**
     * Find the parent file, and set its timestamp to the current time.  This
     * timestamp will be used for populating the Last-Modified header.
     * @param id the id
    **/
    protected void touchParent( final String id ) {
        if (!isRoot(id)) {
            final File file = fileFor(id);
            final File parent = file.getParentFile();
            parent.setLastModified(currentTimeMillis());
        }
    }

    /* Overriding so unit test can mock. */
    @Override
    @VisibleForTesting
    protected File fileFor( final String id ) {
        return super.fileFor(id);
    }
    @Override
    @VisibleForTesting
    protected DocumentReader readDocument( final Document document ) {
        return super.readDocument(document);
    }

    /* Overriding to make the FedoraFileSystemConnector is always read-only. */
    @Override
    public boolean isReadonly() {
        return true;
    }

    @Override
    public boolean isContentNode(final String id) {
        return super.isContentNode(id);
    }

}
