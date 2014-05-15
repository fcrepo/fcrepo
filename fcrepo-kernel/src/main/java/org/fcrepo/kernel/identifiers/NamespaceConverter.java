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
package org.fcrepo.kernel.identifiers;

import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.fcrepo.kernel.rdf.JcrRdfTools.jcrNamespacesToRDFNamespaces;
import static org.fcrepo.kernel.rdf.JcrRdfTools.rdfNamespacesToJcrNamespaces;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * A simple {@link InternalIdentifierConverter} that replaces internal JCR
 * namespaces with external namespaces, and replaces the term for content.
 *
 * @author ajs6f
 * @since Apr 1, 2014
 */
public class NamespaceConverter extends InternalIdentifierConverter {

    private static final Logger log = getLogger(NamespaceConverter.class);

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.identifiers.InternalIdentifierConverter#doForward(
     * java.lang.String)
     */
    @Override
    protected String doForward(final String inputId) {
        log.trace("Converting identifier {} from internal to external...", inputId);
        String result = inputId;
        for (final String jcrNamespace : jcrNamespacesToRDFNamespaces.keySet()) {
            log.trace("Replacing namespace: {} with: {}", jcrNamespace, jcrNamespacesToRDFNamespaces.get(jcrNamespace));
            result = result.replace(jcrNamespace, jcrNamespacesToRDFNamespaces.get(jcrNamespace));
        }
        if (result.endsWith(JCR_CONTENT)) {
            result = result.replace(JCR_CONTENT, FCR_CONTENT);
        }
        log.trace("Converted identifier {} from internal to external {}...", inputId, result);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.identifiers.InternalIdentifierConverter#doBackward
     * (java.lang.String)
     */
    @Override
    protected String doBackward(final String b) {
        log.trace("Converting identifier from external to internal...");
        String result = b;
        for (final String rdfNamespace : rdfNamespacesToJcrNamespaces.keySet()) {
            log.trace("Replacing namespace: {} with: {}", rdfNamespace, rdfNamespacesToJcrNamespaces.get(rdfNamespace));
            result = result.replace(rdfNamespace, rdfNamespacesToJcrNamespaces.get(rdfNamespace));
        }
        if (result.endsWith(FCR_CONTENT)) {
            result = result.replace(FCR_CONTENT, JCR_CONTENT);
        }
        return result;
    }
}
