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

import static org.fcrepo.kernel.api.utils.SubjectMappingUtil.mapSubject;
import static java.util.stream.Stream.empty;
import static org.fcrepo.kernel.api.RequiredRdfContext.MINIMAL;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isNonRdfSourceDescription;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Abstraction for a Fedora datastream backed by a JCR node.
 *
 * @author ajs6f
 * @since Feb 21, 2013
 */
public class NonRdfSourceDescriptionImpl extends FedoraResourceImpl implements NonRdfSourceDescription {

    private static final Logger LOGGER = getLogger(NonRdfSourceDescriptionImpl.class);

    /**
     * The JCR node for this datastream
     *
     * @param n an existing {@link Node}
     */
    public NonRdfSourceDescriptionImpl(final Node n) {
        super(n);
    }

    @Override
    public FedoraResource getDescribedResource() {
        return new FedoraBinaryImpl(getContentNode());
    }

    @Override
    public FedoraResource getBaseVersion() {
        LOGGER.warn("Review if method (getBaseVersion) can be removed after implementing Memento!");
        return null;
    }

    private Node getContentNode() {
        LOGGER.trace("Retrieved datastream content node.");
        try {
            if (isMemento()) {
                final String mementoName = node.getName();
                return node.getNode("../../../" + LDPCV_TIME_MAP + "/" + mementoName);
            }
            return node.getParent();
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final Set<? extends TripleCategory> contexts) {
        final FedoraResource described = getOriginalResource().getDescribedResource();

        final org.apache.jena.graph.Node describedNode = idTranslator.reverse().convert(described).asNode();
        final String resourceUri = idTranslator.reverse().convert(this).getURI();

        return new DefaultRdfStream(idTranslator.reverse().convert(described).asNode(), contexts.stream()
                .filter(contextMap::containsKey)
                .map(x -> contextMap.get(x).apply(this).apply(idTranslator).apply(contexts.contains(MINIMAL)))
                .reduce(empty(), Stream::concat)
                .map(t -> mapSubject(t, resourceUri, describedNode)));
    }

    /**
     * Check if the node has a fedora:datastream mixin
     *
     * @param node node to check
     * @return whether the node has a fedora:datastream mixin
     */
    public static boolean hasMixin(final Node node) {
        return isNonRdfSourceDescription.test(node);
    }

    /**
     * Overrides the superclass to propagate updates to certain properties to the binary if explicitly set.
     */
    @Override
    public void touch(final boolean includeMembershipResource, final Calendar createdDate, final String createdUser,
                      final Calendar modifiedDate, final String modifyingUser) throws RepositoryException {
        super.touch(includeMembershipResource, createdDate, createdUser, modifiedDate, modifyingUser);
        if (createdDate != null || createdUser != null || modifiedDate != null || modifyingUser != null) {
            ((FedoraBinaryImpl) getDescribedResource()).touch(false, createdDate, createdUser,
                    modifiedDate, modifyingUser);
        }
    }

}
