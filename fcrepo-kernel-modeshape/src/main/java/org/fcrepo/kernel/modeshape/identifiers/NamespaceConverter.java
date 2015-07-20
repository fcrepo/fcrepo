/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape.identifiers;

import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.jcrNamespacesToRDFNamespaces;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.rdfNamespacesToJcrNamespaces;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.api.identifiers.InternalIdentifierConverter;
import org.slf4j.Logger;

/**
 * A simple {@link InternalIdentifierConverter} that replaces internal JCR
 * namespaces with external namespaces, and replaces the term for content.
 *
 * @author ajs6f
 * @since Apr 1, 2014
 */
public class NamespaceConverter extends InternalIdentifierConverter {

    private static final Logger LOGGER = getLogger(NamespaceConverter.class);

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.api.identifiers.InternalIdentifierConverter#doForward(
     * java.lang.String)
     */
    @Override
    protected String doForward(final String inputId) {
        LOGGER.trace("Converting identifier {} from internal to external...", inputId);
        String result = inputId;
        for (final String jcrNamespace : jcrNamespacesToRDFNamespaces.keySet()) {
            LOGGER.trace("Replacing namespace: {} with: {}", jcrNamespace, jcrNamespacesToRDFNamespaces
                    .get(jcrNamespace));
            result = result.replace(jcrNamespace, jcrNamespacesToRDFNamespaces.get(jcrNamespace));
        }
        LOGGER.trace("Converted identifier {} from internal to external {}...", inputId, result);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.api.identifiers.InternalIdentifierConverter#doBackward
     * (java.lang.String)
     */
    @Override
    protected String doBackward(final String b) {
        LOGGER.trace("Converting identifier from external to internal...");
        String result = b;
        for (final String rdfNamespace : rdfNamespacesToJcrNamespaces.keySet()) {
            LOGGER.trace("Replacing namespace: {} with: {}", rdfNamespace, rdfNamespacesToJcrNamespaces
                    .get(rdfNamespace));
            result = result.replace(rdfNamespace, rdfNamespacesToJcrNamespaces.get(rdfNamespace));
        }
        return result;
    }
}
