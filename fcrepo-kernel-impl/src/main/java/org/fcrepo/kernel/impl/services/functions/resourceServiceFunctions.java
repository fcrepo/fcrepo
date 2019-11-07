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
package org.fcrepo.kernel.impl.services.functions;

import static org.fcrepo.kernel.api.RdfLexicon.DEFAULT_INTERACTION_MODEL;
import static org.fcrepo.kernel.api.RdfLexicon.INTERACTION_MODELS_FULL;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;

import org.fcrepo.kernel.api.exception.RequestWithAclLinkHeaderException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;

import javax.ws.rs.core.Link;

import java.util.Arrays;
import java.util.List;

public class resourceServiceFunctions {

    /**
     * Utility to determine the correct interaction model from elements of a request.
     *
     * @param linkTypes Link headers with rel="type"
     * @param isRdfContentType Is the Content-type a known RDF type?
     * @param contentPresent Is there content present on the request body?
     * @param isExternalContent Is there Link headers that define external content?
     * @return The determined or default interaction model.
     */
    public static String determineInteractionModel(final List<String> linkTypes,
                                                   final boolean isRdfContentType, final boolean contentPresent,
                                                   final boolean isExternalContent) {
        final String interactionModel = linkTypes == null ? null : linkTypes.stream().filter(INTERACTION_MODELS_FULL::contains).findFirst()
                .orElse(null);

        // If you define a valid interaction model, we try to use it.
        if (interactionModel != null) {
            return interactionModel;
        }
        if (isExternalContent || (contentPresent && !isRdfContentType)) {
            return NON_RDF_SOURCE.toString();
        } else {
            return DEFAULT_INTERACTION_MODEL.toString();
        }
    }

    /**
     * Check that we don't try to provide an ACL Link header.
     * @param links list of the link headers provided.
     * @throws RequestWithAclLinkHeaderException If we provide an rel="acl" link header.
     */
    public static void checkAclLinkHeader(final List<String> links) throws RequestWithAclLinkHeaderException {
        if (links != null && links.stream().anyMatch(l -> Link.valueOf(l).getRel().equals("acl"))) {
            throw new RequestWithAclLinkHeaderException(
                    "Unable to handle request with the specified LDP-RS as the ACL.");
        }
    }

    /**
     * Check if a path has a segment prefixed with fedora:
     *
     * @param externalPath the path.
     */
    public static void hasRestrictedPath(final String externalPath) {
        final String[] pathSegments = externalPath.split("/");
        if (Arrays.stream(pathSegments).anyMatch(p -> p.startsWith("fedora:"))) {
            throw new ServerManagedTypeException("Path cannot contain a fedora: prefixed segment.");
        }
    }

    /**
     * Private constructor.
     */
    private resourceServiceFunctions() {
        // This function left intentionally blank.
    }
}
