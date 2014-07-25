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
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.io.IOException;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import org.infinispan.schematic.document.Document;
import org.modeshape.connector.filesystem.ExternalJsonSidecarExtraPropertyStore;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentReader;
import org.modeshape.jcr.federation.spi.DocumentWriter;
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
                           final NodeTypeManager nodeTypeManager) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);

        if (!isReadonly()) {
            throw new RepositoryException("The " + getClass().getName() + " must have \"readonly\" set to true!");
        }

        if (propertiesDirectoryPath != null) {
           propertiesDirectory = new File(propertiesDirectoryPath);
            if (!propertiesDirectory.exists() || !propertiesDirectory.isDirectory()) {
                throw new RepositoryException("Configured \"propertiesDirectory\", " + propertiesDirectoryPath
                        + ", does not exist or is not a directory.");
            }
            if (extraPropertiesStore() != null) {
                LOGGER.warn("Extra properties store was specified but won't be used!");
            }
            setExtraPropertiesStore(new ExternalJsonSidecarExtraPropertyStore(this, translator(), propertiesDirectory));
        }
    }

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
        }

        // Persist new properties (if allowed)
        if (shouldCacheProperties()) {
            saveProperties(docReader);
        }

        return docWriter.document();
    }

    /**
     * Checks whether internally managed properties can and should be stored to
     * an ExtraPropertiesStore.
     */
    protected boolean shouldCacheProperties() {
        return extraPropertiesStore() != null && (!isReadonly() || this.propertiesDirectory != null);
    }

    @Override
    public String sha1(final File file) {
        final String cachedSha1 = getCachedSha1(file);
        if (cachedSha1 == null) {
            return computeAndCacheSha1(file);
        } else {
            return cachedSha1;
        }
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
            final Map<Name, Property> updateMap = new HashMap<Name, Property>();
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



    private static void decorateDatastreamNode(final DocumentReader docReader, final DocumentWriter docWriter) {
        if (!docReader.getMixinTypeNames().contains(FEDORA_DATASTREAM)) {
            LOGGER.trace("Adding mixin: {}, to {}", FEDORA_DATASTREAM, docReader.getDocumentId());
            docWriter.addMixinType(FEDORA_DATASTREAM);
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
        } else {
            final DateTime datetime = (DateTime) lastModified.getFirstValue();
            if (datetime.toDate().equals(new Date(file.lastModified()))) {
                return false;
            } else {
                LOGGER.trace("{} has been modified ({}) since hash was last computed ({}).", file.getName(),
                        new Date(file.lastModified()), datetime.toDate());
                return true;
            }
        }
    }

    private static BinaryValue getBinaryValue(final DocumentReader docReader) {
        final Property binaryProperty = docReader.getProperty(JCR_DATA);
        return (BinaryValue) binaryProperty.getFirstValue();
    }

    private void saveProperties(final DocumentReader docReader) {
        LOGGER.trace("Persisting properties for {}", docReader.getDocumentId());
        final Map<Name, Property> properties = docReader.getProperties();
        final ExtraProperties extraProperties = extraPropertiesFor(docReader.getDocumentId(), true);
        extraProperties.addAll(properties).except(JCR_PRIMARY_TYPE, JCR_DATA, CONTENT_SIZE);
        extraProperties.save();
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
    **/
    protected void touchParent( final String id ) {
        if ( !isRoot(id) ) {
            final File file = fileFor(id);
            final File parent = file.getParentFile();
            parent.setLastModified( System.currentTimeMillis() );
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
}
