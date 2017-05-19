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

import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isNonRdfSourceDescription;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.slf4j.Logger;

import java.util.Calendar;

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
        try {
            return new NonRdfSourceDescriptionImpl(getVersionManager().getBaseVersion(getPath()).getFrozenNode());
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private Node getContentNode() {
        LOGGER.trace("Retrieved datastream content node.");
        try {
            return node.getNode(JCR_CONTENT);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
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
    public void touch(final boolean includeMembershipResource, final Calendar createdDate, final String createdUser,
                      final Calendar modifiedDate, final String modifyingUser) throws RepositoryException {
        super.touch(includeMembershipResource, createdDate, createdUser, modifiedDate, modifyingUser);
        if (createdDate != null || createdUser != null || modifiedDate != null || modifyingUser != null) {
            ((FedoraBinaryImpl) getDescribedResource()).touch(false, createdDate, createdUser,
                    modifiedDate, modifyingUser);
        }
    }

}
