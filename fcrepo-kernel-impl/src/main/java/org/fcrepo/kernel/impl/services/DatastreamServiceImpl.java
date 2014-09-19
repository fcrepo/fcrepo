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
import static com.google.common.collect.ImmutableSet.copyOf;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.metrics.RegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;

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
     * Get the resource at the given path as a binary
     * @param session
     * @param path
     * @return
     */
    @Override
    public FedoraBinary getBinary(final Session session, final String path) {
        return getDatastream(session, path).getBinary();
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
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param node datastream node
     * @return node as datastream
     */
    @Override
    public FedoraBinary asBinary(final Node node) {
        return new FedoraBinaryImpl(node);
    }


    /**
     * Get the fixity results for the datastream as a RDF Dataset
     *
     * @param subjects
     * @param binary
     * @return fixity results
     * @throws RepositoryException
     */
    @Override
    public RdfStream getFixityResultsModel(final IdentifierTranslator subjects,
            final FedoraBinary binary) {
        try {

            final URI digestUri = binary.getContentDigest();
            final long size = binary.getContentSize();
            final String algorithm = ContentDigest.getAlgorithm(digestUri);

            final Collection<FixityResult> blobs = runFixity(binary, algorithm);

            return JcrRdfTools.withContext(subjects,binary.getNode().getSession())
                    .getJcrTriples(binary.getNode(), blobs, digestUri, size)
                    .topic(subjects.getSubject(binary.getPath())
                            .asNode());
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Run the fixity check on the datastream and attempt to automatically
     * correct failures if additional copies of the bitstream are available
     *
     * @param binary
     * @return results
     * @throws RepositoryException
     */
    private Collection<FixityResult> runFixity(final FedoraBinary binary, final String algorithm) {

        Set<FixityResult> fixityResults;

        fixityCheckCounter.inc();

        final Timer.Context context = timer.time();

        try {
            fixityResults =
                    copyOf(binary.getFixity(repo, algorithm));

        } finally {
            context.stop();
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
    @Override
    public StoragePolicyDecisionPoint getStoragePolicyDecisionPoint() {

        return storagePolicyDecisionPoint;
    }

}
