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

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.utils.MessageExternalBodyContentType;
import org.fcrepo.kernel.modeshape.utils.impl.CacheEntryFactory;
import org.slf4j.Logger;

import com.codahale.metrics.Timer;

/**
 * External binary from a uri
 *
 * @author bbpennel
 * @since 12/14/2017
 */
public class UriBinaryImpl extends FedoraBinaryImpl {
    private static final Logger LOGGER = getLogger(UriBinaryImpl.class);

    /**
     * Construct UriBinaryImpl
     *
     * @param node node
     */
    public UriBinaryImpl(final Node node) {
        super(node);
    }

    @Override
    public Collection<URI> checkFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final Collection<String> algorithms)
            throws UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        fixityCheckCounter.inc();

        try (final Timer.Context context = timer.time()) {

            final String mimeType = getMimeType();
            final MessageExternalBodyContentType externalBody = MessageExternalBodyContentType.parse(mimeType);
            final String resourceLocation = externalBody.getResourceLocation();
            LOGGER.debug("Checking external resource: " + resourceLocation);
            return CacheEntryFactory.forProperty(getProperty(HAS_MIME_TYPE)).checkFixity(algorithms);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
