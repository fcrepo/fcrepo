/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.services;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.difference;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.sparql.util.Symbol;
import org.fcrepo.Datastream;
import org.fcrepo.binary.PolicyDecisionPoint;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.JcrRdfTools;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStoreFactory;

/**
 * Service for creating and retrieving Datastreams without using the JCR API.
 *
 * @author cbeer
 * @date Feb 11, 2013
 */
@Component
public class DatastreamService extends RepositoryService {

    @Autowired(required = false)
    PolicyDecisionPoint storagePolicyDecisionPoint;

    @Autowired
    private LowLevelStorageService llStoreService;

    static final Counter fixityCheckCounter =
        getMetrics().counter(name(LowLevelStorageService.class,
                             "fixity-check-counter"));

    static final Timer timer = getMetrics().timer(name(Datastream.class,
                                                  "fixity-check-time"));

    static final Counter fixityRepairedCounter =
        getMetrics().counter(name(LowLevelStorageService.class,
                             "fixity-repaired-counter"));

    static final Counter fixityErrorCounter =
        getMetrics().counter(name(LowLevelStorageService.class,
                             "fixity-error-counter"));


    private static final Logger logger = getLogger(DatastreamService.class);


    /**
     * Create a new Datastream node in the JCR store
     * @param session the jcr session to use
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param requestBodyStream binary payload for the datastream
     * @return
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException
     */
    public Node createDatastreamNode(final Session session,
                                     final String dsPath,
                                     final String contentType,
                                     final InputStream requestBodyStream)
        throws RepositoryException, IOException, InvalidChecksumException {

        return createDatastreamNode(session, dsPath, contentType,
                                    requestBodyStream, null);
    }

    /**
     * Create a new Datastream node in the JCR store
     * @param session the jcr session to use
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param requestBodyStream binary payload for the datastream
     * @param checksum the digest for the binary payload (as urn:sha1:xyz)
     * @return
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException
     */
    public Node createDatastreamNode(final Session session,
                                     final String dsPath,
                                     final String contentType,
                                     final InputStream requestBodyStream,
                                     final URI checksum)
        throws RepositoryException, IOException, InvalidChecksumException {

        final Datastream ds = new Datastream(session, dsPath);
        ds.setContent(requestBodyStream, contentType, checksum,
                      getStoragePolicyDecisionPoint());
        return ds.getNode();
    }

    /**
     * Retrieve the JCR node for a Datastream by pid and dsid
     * @param path
     * @return
     * @throws RepositoryException
     */
    public Node getDatastreamNode(final Session session, final String path)
        throws RepositoryException {
        logger.trace("Executing getDatastreamNode() with path: {}",
                     path);
        final Node dsNode = getDatastream(session, path).getNode();
        logger.trace("Retrieved datastream node: {}", dsNode.getName());
        return dsNode;
    }

    /**
     * Retrieve a Datastream instance by pid and dsid
     * @param path jcr path to the datastream
     * @return
     * @throws RepositoryException
     */
    public Datastream getDatastream(final Session session, final String path)
        throws RepositoryException {
        return new Datastream(session, path);
    }

    /**
     * Retrieve a Datastream instance by pid and dsid
     * @param node datastream node
     * @return
     * @throws RepositoryException
     */
    public Datastream asDatastream(final Node node)
        throws RepositoryException {
        return new Datastream(node);
    }

    /**
     * Get the fixity results for the datastream as a RDF Dataset
     * @param subjects
     * @param datastream
     * @return
     * @throws RepositoryException
     */
    public Dataset getFixityResultsModel(final GraphSubjects subjects,
                                         final Datastream datastream)
        throws RepositoryException {


        final Collection<FixityResult> blobs = runFixityAndFixProblems(datastream);


        final Model model = JcrRdfTools.getFixityResultsModel(subjects, datastream.getNode(), blobs);


        final Dataset dataset = GraphStoreFactory.create(model).toDataset();

        String uri = JcrRdfTools.getGraphSubject(subjects, datastream.getNode()).getURI();
        com.hp.hpl.jena.sparql.util.Context context = dataset.getContext();
        context.set(Symbol.create("uri"),uri);

        return dataset;
    }

    /**
     * Run the fixity check on the datastream and attempt to automatically
     * correct failures if additional copies of the bitstream are available
     *
     * @param datastream
     * @return
     * @throws RepositoryException
     */
    public Collection<FixityResult> runFixityAndFixProblems(final Datastream datastream)
        throws RepositoryException {

        Set<FixityResult> fixityResults;
        Set<FixityResult> goodEntries;
        final URI digestUri = datastream.getContentDigest();
        final long size = datastream.getContentSize();

        fixityCheckCounter.inc();

        final Timer.Context context = timer.time();

        try {
            fixityResults =
                copyOf(getFixity(datastream.getNode().
                                 getNode(JcrConstants.JCR_CONTENT),
                                 digestUri, size));

            goodEntries = ImmutableSet.copyOf(Collections2.filter(fixityResults, new Predicate<FixityResult>() {
                @Override
                public boolean apply(org.fcrepo.utils.FixityResult input) {
                    return input.matches(size, digestUri);
                }
            }));

        } finally {
            context.stop();
        }

        if (goodEntries.size() == 0) {
            logger.error("ALL COPIES OF " +
                         datastream.getNode().getPath() +
                         " HAVE FAILED FIXITY CHECKS.");
            return fixityResults;
        }

        final LowLevelCacheEntry anyGoodCacheEntry =
            goodEntries.iterator().next().getEntry();

        final Set<FixityResult> badEntries =
            difference(fixityResults, goodEntries);

        for (final FixityResult result : badEntries) {
            try {
                result.getEntry()
                    .storeValue(anyGoodCacheEntry.getInputStream());
                final FixityResult newResult =
                    result.getEntry().checkFixity(digestUri, size);
                if (newResult.isSuccess()) {
                    result.setRepaired();
                    fixityRepairedCounter.inc();
                } else {
                    fixityErrorCounter.inc();
                }
            } catch (final IOException e) {
                logger.warn("Exception repairing low-level cache entry: {}", e);
            }
        }

        return fixityResults;
    }

    /**
     * Get the fixity results for this datastream's bitstream, and compare it
     * against the given checksum and size.
     *
     * @param resource
     * @param dsChecksum -the checksum and algorithm represented as a URI
     * @param dsSize
     * @return
     * @throws RepositoryException
     */
    public Collection<FixityResult> getFixity(final Node resource,
                                              final URI dsChecksum,
                                              final long dsSize)
        throws RepositoryException {
        logger.debug("Checking resource: " + resource.getPath());
        Function<LowLevelCacheEntry, FixityResult> checkCacheFunc =
            ServiceHelpers.getCheckCacheFixityFunction(dsChecksum, dsSize);
        return llStoreService.
            transformLowLevelCacheEntries(resource, checkCacheFunc);
    }

    /**
     * Set the low-level storage service (if Spring didn't wire it in)
     * @param llStoreService
     */
    public void setLlStoreService(final LowLevelStorageService llStoreService) {
        this.llStoreService = llStoreService;
    }


    /**
     * Set the storage policy decision point
     * (if Spring didn't wire it in for us)
     *
     * @param pdp
     */
    public void setStoragePolicyDecisionPoint(PolicyDecisionPoint pdp) {
        this.storagePolicyDecisionPoint = pdp;
    }

    /**
     * Get the Policy Decision Point for this service.
     * Initialize it if Spring didn't wire it in for us.
     *
     * @return a PolicyDecisionPoint
     */
    private PolicyDecisionPoint getStoragePolicyDecisionPoint() {
        if (storagePolicyDecisionPoint == null) {
            storagePolicyDecisionPoint = new PolicyDecisionPoint();
        }

        return storagePolicyDecisionPoint;
    }

}
