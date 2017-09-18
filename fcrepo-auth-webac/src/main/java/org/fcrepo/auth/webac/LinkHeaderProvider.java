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
package org.fcrepo.auth.webac;

import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESS_CONTROL_VALUE;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.api.UriAwareHttpHeaderFactory;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;

import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.apache.jena.rdf.model.Resource;

import org.springframework.stereotype.Component;

/**
 * Insert WebAC Link headers to responses
 *
 * @author whikloj
 * @since 2015-10-30
 */
@Component
public class LinkHeaderProvider implements UriAwareHttpHeaderFactory {

    private static final Logger LOGGER = getLogger(LinkHeaderProvider.class);

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    private NodeService nodeService;

    @Override
    public Multimap<String, String> createHttpHeadersForResource(final UriInfo uriInfo, final FedoraResource resource) {

        final FedoraSession internalSession = sessionFactory.getInternalSession();
        final IdentifierConverter<Resource, FedoraResource> translator =
                new DefaultIdentifierTranslator(getJcrSession(internalSession));
        final ListMultimap<String, String> headers = ArrayListMultimap.create();

        LOGGER.debug("Adding WebAC Link Header for Resource: {}", resource.getPath());
        // Get the correct Acl for this resource
        WebACRolesProvider.getEffectiveAcl(resource).ifPresent(acls -> {
            // If the Acl is present we need to use the internal session to get its URI
            nodeService.find(internalSession, acls.resource.getPath())
            .getTriples(translator, PROPERTIES)
            .collect(toModel()).listObjectsOfProperty(createProperty(WEBAC_ACCESS_CONTROL_VALUE))
            .forEachRemaining(linkObj -> {
                if (linkObj.isURIResource()) {
                    final Resource acl = linkObj.asResource();
                    final String aclPath = translator.convert(acl).getPath();
                    final URI aclUri = uriInfo.getBaseUriBuilder().path(aclPath).build();
                    headers.put("Link", Link.fromUri(aclUri).rel("acl").build().toString());
                }
            });
        });

        return headers;
    }


}
