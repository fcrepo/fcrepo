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
package org.fcrepo.kernel.modeshape;

import static org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM.SHA1;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.api.utils.CacheEntry;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.kernel.api.utils.FixityResult;
import org.fcrepo.kernel.modeshape.rdf.impl.FixityRdfContext;
import org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils;
import org.fcrepo.kernel.modeshape.utils.impl.CacheEntryFactory;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.ValueFactory;
import org.slf4j.Logger;

import com.codahale.metrics.Timer;

/**
 * Fedora Binary stored internally in modeshape
 *
 * @author bbpennel
 */
public class InternalFedoraBinary extends AbstractFedoraBinary {

    private static final Logger LOGGER = getLogger(InternalFedoraBinary.class);

    /**
     * Construct InternalFedoraBinary
     *
     * @param node node
     */
    public InternalFedoraBinary(final Node node) {
        super(node);

        if (node.isNew()) {
            initializeNewBinaryProperties();
        }
    }

    private void initializeNewBinaryProperties() {
        try {
            decorateContentNode(node, new HashSet<>());
        } catch (final RepositoryException e) {
            LOGGER.warn("Count not decorate {} with FedoraBinary properties: {}", node, e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getContent()
     */
    @Override
    public InputStream getContent() {
        try {
            return getBinaryContent().getStream();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Retrieve the JCR Binary object
     *
     * @return a JCR-wrapped Binary object
     */
    private javax.jcr.Binary getBinaryContent() {
        try {
            return getProperty(JCR_DATA).getBinary();
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#setContent(java.io.InputStream, java.lang.String, java.net.URI,
     * java.lang.String, org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint)
     */
    @Override
    public void setContent(final InputStream content, final String contentType,
            final Collection<URI> checksums, final String originalFileName,
            final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException {

        try {
            final Node contentNode = getNode();

            if (contentNode.canAddMixin(FEDORA_BINARY)) {
                contentNode.addMixin(FEDORA_BINARY);
            }

            if (contentType != null) {
                contentNode.setProperty(HAS_MIME_TYPE, contentType);
            }

            if (originalFileName != null) {
                contentNode.setProperty(FILENAME, originalFileName);
            }

            LOGGER.debug("Created content node at path: {}", contentNode.getPath());

            String hint = null;

            if (storagePolicyDecisionPoint != null) {
                hint = storagePolicyDecisionPoint.evaluatePolicies(this);
            }
            final ValueFactory modevf =
                    (ValueFactory) node.getSession().getValueFactory();
            final Binary binary = modevf.createBinary(content, hint);

            /*
             * This next line of code deserves explanation. If we chose for the
             * simpler line: Property dataProperty =
             * contentNode.setProperty(JCR_DATA, requestBodyStream); then the JCR
             * would not block on the stream's completion, and we would return to
             * the requester before the mutation to the repo had actually completed.
             * So instead we use createBinary(requestBodyStream), because its
             * contract specifies: "The passed InputStream is closed before this
             * method returns either normally or because of an exception." which
             * lets us block and not return until the job is done! The simpler code
             * may still be useful to us for an asynchronous method that we develop
             * later.
             */
            final Property dataProperty = contentNode.setProperty(JCR_DATA, binary);

            // Ensure provided checksums are valid
            final Collection<URI> nonNullChecksums = (null == checksums) ? new HashSet<>() : checksums;
            verifyChecksums(nonNullChecksums, dataProperty);

            decorateContentNode(contentNode, nonNullChecksums);
            FedoraTypesUtils.touch(getNode());
            FedoraTypesUtils.touch(((FedoraResourceImpl) getDescription()).getNode());

            LOGGER.debug("Created data property at path: {}", dataProperty.getPath());

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getMimeType()
     */
    @Override
    public String getMimeType() {
        final String mimeType = getMimeTypeValue();
        if (mimeType == null) {
            return "application/octet-stream";
        } else {
            return mimeType;
        }
    }

    /**
     * This method ensures that the arg checksums are valid against the binary associated with the arg dataProperty.
     * If one or more of the checksums are invalid, an InvalidChecksumException is thrown.
     *
     * @param checksums that the user provided
     * @param dataProperty containing the binary against which the checksums will be verified
     * @throws InvalidChecksumException
     * @throws RepositoryException
     */
    private void verifyChecksums(final Collection<URI> checksums, final Property dataProperty)
            throws InvalidChecksumException, RepositoryException {

        final Map<URI, URI> checksumErrors = new HashMap<>();

        // Loop through provided checksums validating against computed values
        checksums.forEach(checksum -> {
            final String algorithm = ContentDigest.getAlgorithm(checksum);
            try {
                // The case internally supported by ModeShape
                if (algorithm.equals(SHA1.algorithm)) {
                    final String dsSHA1 = ((Binary) dataProperty.getBinary()).getHexHash();
                    final URI dsSHA1Uri = ContentDigest.asURI(SHA1.algorithm, dsSHA1);

                    if (!dsSHA1Uri.equals(checksum)) {
                        LOGGER.debug("Failed checksum test");
                        checksumErrors.put(checksum, dsSHA1Uri);
                    }

                    // The case that requires re-computing the checksum
                } else {
                    final CacheEntry cacheEntry = CacheEntryFactory.forProperty(dataProperty);
                    cacheEntry.checkFixity(algorithm).stream().findFirst().ifPresent(
                            fixityResult -> {
                                if (!fixityResult.matches(checksum)) {
                                    LOGGER.debug("Failed checksum test");
                                    checksumErrors.put(checksum, fixityResult.getComputedChecksum());
                                }
                            });
                }
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        });

        // Throw an exception if any checksum errors occurred
        if (!checksumErrors.isEmpty()) {
            final String template = "Checksum Mismatch of %1$s and %2$s\n";
            final StringBuilder error = new StringBuilder();
            checksumErrors.forEach((key, value) -> error.append(String.format(template, key, value)));
            throw new InvalidChecksumException(error.toString());
        }

    }

    @Override
    public RdfStream getFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final URI digestUri,
            final long size) {

        fixityCheckCounter.inc();

        try (final Timer.Context context = timer.time()) {

            LOGGER.debug("Checking resource: " + getPath());

            final String algorithm = ContentDigest.getAlgorithm(digestUri);

            final long contentSize = size < 0 ? getBinaryContent().getSize() : size;

            final Collection<FixityResult> fixityResults = CacheEntryFactory.forProperty(getProperty(JCR_DATA))
                    .checkFixity(algorithm);

            return new FixityRdfContext(this, idTranslator, fixityResults, digestUri, contentSize);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public Collection<URI> checkFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final Collection<String> algorithms)
            throws UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        fixityCheckCounter.inc();

        try (final Timer.Context context = timer.time()) {

            LOGGER.debug("Checking resource: " + getPath());
            return CacheEntryFactory.forProperty(getProperty(JCR_DATA)).checkFixity(algorithms);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private static void decorateContentNode(final Node contentNode, final Collection<URI> checksums)
            throws RepositoryException {
        if (contentNode == null) {
            LOGGER.warn("{} node appears to be null!", JCR_CONTENT);
            return;
        }
        if (contentNode.canAddMixin(FEDORA_BINARY)) {
            contentNode.addMixin(FEDORA_BINARY);
        }

        if (contentNode.hasProperty(JCR_DATA)) {
            final Property dataProperty = contentNode.getProperty(JCR_DATA);
            final Binary binary = (Binary) dataProperty.getBinary();
            final String dsChecksum = binary.getHexHash();

            contentSizeHistogram.update(dataProperty.getLength());

            checksums.add(ContentDigest.asURI(SHA1.algorithm, dsChecksum));

            final String[] checksumArray = new String[checksums.size()];
            checksums.stream().map(Object::toString).collect(Collectors.toSet()).toArray(checksumArray);

            contentNode.setProperty(CONTENT_DIGEST, checksumArray);
            contentNode.setProperty(CONTENT_SIZE, dataProperty.getLength());

            LOGGER.debug("Decorated data property at path: {}", dataProperty.getPath());
        }
    }
}
