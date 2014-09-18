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
package org.fcrepo.kernel.impl.services;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.metrics.RegistryService;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.common.base.Predicate;

/**
 * Service for creating and retrieving Datastreams without using the JCR API.
 *
 * @author cbeer
 * @since Feb 11, 2013
 */
@Component
public class DatastreamServiceImpl extends AbstractService implements DatastreamService {

    @Autowired(required = false)
    StoragePolicyDecisionPoint storagePolicyDecisionPoint;

    static final RegistryService registryService = RegistryService.getInstance();
    static final Counter fixityCheckCounter
        = registryService.getMetrics().counter(name(DatastreamService.class, "fixity-check-counter"));

    static final Timer timer = registryService.getMetrics().timer(
            name(Datastream.class, "fixity-check-time"));

    private static final Logger LOGGER = getLogger(DatastreamService.class);

    /**
     * Create a new Datastream node in the JCR store
     *
     * @param session the jcr session to use
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param originalFileName the original file name for the input stream
     * @param requestBodyStream binary payload for the datastream
     * @return created datastream
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    @Override
    public Datastream createDatastream(final Session session,
            final String dsPath, final String contentType,
            final String originalFileName,
            final InputStream requestBodyStream) throws RepositoryException,
            InvalidChecksumException {

        return createDatastream(session, dsPath, contentType,
                                       originalFileName, requestBodyStream, null);
    }

    /**
     * Create a new Datastream node in the JCR store
     *
     *
     * @param session the jcr session to use
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param originalFileName the original file name for the input stream
     * @param content binary payload for the datastream
     * @param checksum the digest for the binary payload (as urn:sha1:xyz)   @return
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    @Override
    public Datastream createDatastream(final Session session,
                                     final String dsPath, final String contentType,
                                     final String originalFileName, final InputStream content,
                                     final URI checksum)
        throws InvalidChecksumException {

        final Datastream ds = getDatastream(session, dsPath);
        ds.setContent(content, contentType, checksum,
                         originalFileName, getStoragePolicyDecisionPoint());
        return ds;
    }

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param path jcr path to the datastream
     * @return datastream
     * @throws RepositoryException
     */
    @Override
    public Datastream getDatastream(final Session session, final String path) {
        return new DatastreamImpl(session, path);
    }

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param node datastream node
     * @return node as datastream
     */
    @Override
    public Datastream asDatastream(final Node node) {
        return new DatastreamImpl(node);
    }

    /**
     * Get the fixity results for the datastream as a RDF Dataset
     *
     * @param subjects
     * @param datastream
     * @return fixity results
     * @throws RepositoryException
     */
    @Override
    public RdfStream getFixityResultsModel(final IdentifierTranslator subjects,
            final Datastream datastream) {
        try {
            final Collection<FixityResult> blobs = runFixityAndFixProblems(datastream);

            return JcrRdfTools.withContext(subjects,datastream.getNode().getSession())
                    .getJcrTriples(datastream.getNode(), blobs)
                    .topic(subjects.getSubject(datastream.getPath())
                            .asNode());
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Run the fixity check on the datastream and attempt to automatically
     * correct failures if additional copies of the bitstream are available
     *
     * @param datastream
     * @return results
     * @throws RepositoryException
     */
    @Override
    public Collection<FixityResult> runFixityAndFixProblems(final Datastream datastream) {

        Set<FixityResult> fixityResults;
        Set<FixityResult> goodEntries;
        final URI digestUri = datastream.getContentDigest();
        final long size = datastream.getContentSize();

        fixityCheckCounter.inc();

        final Timer.Context context = timer.time();

        try {
            fixityResults =
                    copyOf(datastream.getFixity(repo, digestUri, size));

            goodEntries =
                    copyOf(filter(fixityResults, new Predicate<FixityResult>() {

                        @Override
                        public boolean apply(
                                final FixityResult input) {
                            return input.matches(size, digestUri);
                        }
                    }));

        } finally {
            context.stop();
        }

        if (goodEntries.isEmpty()) {
            LOGGER.error("ALL COPIES OF " + datastream.getPath() +
                             " HAVE FAILED FIXITY CHECKS.");
        }

        return fixityResults;
    }

    /**
     * Set the storage policy decision point (if Spring didn't wire it in for
     * us)
     *
     * @param pdp
     */
    public void setStoragePolicyDecisionPoint(final StoragePolicyDecisionPoint pdp) {
        this.storagePolicyDecisionPoint = pdp;
    }

    /**
     * Get the StoragePolicy Decision Point for this service.
     *
     * @return a PolicyDecisionPoint
     */
    private StoragePolicyDecisionPoint getStoragePolicyDecisionPoint() {

        return storagePolicyDecisionPoint;
    }

}
