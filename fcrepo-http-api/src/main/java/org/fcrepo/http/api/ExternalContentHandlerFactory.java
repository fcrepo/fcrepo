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
package org.fcrepo.http.api;

import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.stream.Collectors;

import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.slf4j.Logger;

/**
 * Constructs ExternalContentHandler objects from link headers
 *
 * @author bbpennel
 */
public class ExternalContentHandlerFactory {

    private static final Logger LOGGER = getLogger(ExternalContentHandlerFactory.class);

    private ExternalContentPathValidator validator;

    /**
     * Looks for ExternalContent link header and if it finds one it will return a new ExternalContentHandler object
     * based on the found Link header. If multiple external content headers were found or the URI provided in the
     * header is not a valid external content path, then an ExternalMessageBodyException will be thrown.
     *
     * @param links links from the request header
     * @return External Content Handler Object if Link header found, else null
     * @throws ExternalMessageBodyException thrown if more than one external content link was provided, or if the URL
     *         of the header was not a valid external content path.
     */
    public ExternalContentHandler createFromLinks(final List<String> links) throws ExternalMessageBodyException {
        if (links == null) {
            return null;
        }

        final List<String> externalContentLinks = links.stream()
                .filter(x -> x.contains(EXTERNAL_CONTENT.toString()))
                .collect(Collectors.toList());

        if (externalContentLinks.size() > 1) {
            // got a problem, you can only have one ExternalContent links
            throw new ExternalMessageBodyException("More then one External Content Link header in request");
        } else if (externalContentLinks.size() == 1) {
            final String link = externalContentLinks.get(0);
            final String uri = getUriString(link);
            // Validate that the URI is valid according to allowed set of external uris
            try {
                validator.validate(uri);
            } catch (final ExternalMessageBodyException e) {
                LOGGER.warn("Rejected invalid external path {}", uri);
                throw e;
            }

            return new ExternalContentHandler(link);
        }

        return null;
    }

    private static String getUriString(final String link) {
        final String value = link.trim();
        if (value.startsWith("<")) {
            final int gtIndex = value.indexOf('>');
            if (gtIndex != -1) {
                return value.substring(1, gtIndex).trim();
            } else {
                throw new IllegalArgumentException("Missing token > in " + value);
            }
        } else {
            throw new IllegalArgumentException("Missing starting token < in " + value);
        }
    }

    /**
     * Set the external content path validator
     *
     * @param validator validator
     */
    public void setValidator(final ExternalContentPathValidator validator) {
        this.validator = validator;
    }
}
