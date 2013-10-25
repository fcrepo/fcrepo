/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.utils.iterators;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.fcrepo.kernel.utils.JcrRdfTools.getJcrNamespaceForRDFNamespace;
import static org.fcrepo.kernel.utils.NodePropertiesTools.appendOrReplaceNodeProperty;
import static org.fcrepo.kernel.utils.NodePropertiesTools.getPropertyType;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Consumes an {@link RdfStream} by adding its contents to the
 * JCR.
 *
 * @see {@link RdfRemover} for contrast
 * @author ajs6f
 * @date Oct 24, 2013
 */
public class RdfAdder extends PersistingRdfStreamConsumer {

    private static final Logger LOGGER = getLogger(RdfAdder.class);

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public RdfAdder(final GraphSubjects graphSubjects, final Session session,
        final RdfStream stream) {
        super(graphSubjects, session, stream);
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource,
        final Node subjectNode) throws RepositoryException {
        final String namespace = getJcrNamespaceForRDFNamespace(mixinResource.getNameSpace());
        String namespacePrefix = null;
        final Map<String, String> streamNSMap =
            checkNotNull(stream().namespaces(),
                    "Use an empty map of namespaces, not null!");
        if (streamNSMap.containsValue(namespace)) {
            LOGGER.debug("Found namespace: {} in stream namespace mapping.",
                    namespace);
            for (final String prefix : streamNSMap.keySet()) {
                final String streamNamespace = streamNSMap.get(prefix);
                if (namespace.equals(streamNamespace)) {
                    LOGGER.debug(
                            "Found namespace: {} in stream namespace mapping with prefix: {}.",
                            namespace, namespacePrefix);
                    namespacePrefix = prefix;
                }
            }
        } else {
            try {
                namespacePrefix = session().getNamespacePrefix(namespace);
                LOGGER.debug(
                        "Found namespace: {} in repository namespace mapping with prefix: {}.",
                        namespace, namespacePrefix);
            } catch (final NamespaceException e) {
                throw new MalformedRdfException(
                        "Unable to resolve registered namespace for resource "
                                + mixinResource.toString());

            }
        }
        final String mixinName =
            namespacePrefix + ":" + mixinResource.getLocalName();
        LOGGER.debug("Constructed JCR mixin name: {}", mixinName);
        if (session().getWorkspace().getNodeTypeManager()
                .hasNodeType(mixinName)) {
            if (subjectNode.canAddMixin(mixinName)) {
                LOGGER.debug("Adding mixin: {} to node: {}.", mixinName,
                        subjectNode.getPath());
                subjectNode.addMixin(mixinName);
            } else {
                throw new MalformedRdfException(
                        "Could not persist triple containing type assertion:"
                                + mixinResource.toString()
                                + " because no such mixin/type can be added to this node: "
                                + subjectNode.getPath() + "!");
            }
        } else {
            throw new MalformedRdfException(
                    "Could not persist triple containing type assertion:"
                            + mixinResource.toString()
                            + " because no such mixin/type can be found in the repository!");
        }
    }


    @Override
    protected void operateOnProperty(final Statement t, final Node n) throws RepositoryException {
        LOGGER.debug("Adding property from triple: {} to node: {}.", t, n
                .getPath());
        final String propertyName =
            getPropertyNameFromPredicate(n, t.getPredicate());
        final Value v =
            createValue(n, t.getObject(), getPropertyType(n, propertyName));
        appendOrReplaceNodeProperty(n, propertyName, v);
    }
}
