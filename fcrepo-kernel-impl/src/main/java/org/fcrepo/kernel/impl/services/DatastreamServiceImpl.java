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
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.exception.ResourceTypeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.metrics.RegistryService;
import org.slf4j.Logger;
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


    private static final Logger LOGGER = getLogger(DatastreamServiceImpl.class);

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param path jcr path to the datastream
     * @return datastream
     * @throws RepositoryException
     */
    @Override
    public Datastream findOrCreateDatastream(final Session session, final String path) {
        try {
            final Node node = findOrCreateNode(session, path, NT_FOLDER, NT_FILE);

            if (node.isNew()) {
                initializeNewDatastreamProperties(node);
            }

            return asDatastream(node);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private void initializeNewDatastreamProperties(final Node node) {
        try {

            if (node.canAddMixin(FEDORA_RESOURCE)) {
                node.addMixin(FEDORA_RESOURCE);
            }

            if (node.canAddMixin(FEDORA_DATASTREAM)) {
                node.addMixin(FEDORA_DATASTREAM);
            }

            final Node contentNode = findOrCreateChild(node, JCR_CONTENT, NT_RESOURCE);

            if (contentNode.canAddMixin(FEDORA_BINARY)) {
                contentNode.addMixin(FEDORA_BINARY);
            }
        } catch (final RepositoryException e) {
            LOGGER.warn("Could not decorate {} with datastream properties: {}", node, e);
        }

    }

    /**
     * Get the resource at the given path as a binary
     * @param session
     * @param path
     * @return
     */
    @Override
    public FedoraBinary getBinary(final Session session, final String path) {
        return findOrCreateDatastream(session, path).getBinary();
    }

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param node datastream node
     * @return node as datastream
     */
    @Override
    public Datastream asDatastream(final Node node) {
        final DatastreamImpl datastream = new DatastreamImpl(node);
        assertIsType(datastream, FEDORA_DATASTREAM);
        return datastream;
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

    private void assertIsType(final FedoraResource resource, final String type) {
        if (!resource.hasType(type)) {
            throw new ResourceTypeException(resource + " can not be used as a " + type);
        }
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
    public RdfStream getFixityResultsModel(final IdentifierConverter<Resource,Node> subjects,
            final FedoraBinary binary) {
        try {

            final URI digestUri = binary.getContentDigest();
            final long size = binary.getContentSize();
            final String algorithm = ContentDigest.getAlgorithm(digestUri);

            final Collection<FixityResult> blobs = runFixity(binary, algorithm);

            return JcrRdfTools.withContext(subjects,binary.getNode().getSession())
                    .getJcrTriples(binary.getNode(), blobs, digestUri, size)
                    .topic(subjects.reverse().convert(binary.getNode()).asNode());
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
