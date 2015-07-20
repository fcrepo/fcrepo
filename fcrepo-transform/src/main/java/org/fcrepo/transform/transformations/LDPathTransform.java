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
package org.fcrepo.transform.transformations;

import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.backend.jena.GenericJenaBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.transform.Transformation;

import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.WebApplicationException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.collect.ImmutableList.builder;
import static com.google.common.collect.ImmutableSortedSet.orderedBy;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utilities for working with LDPath
 *
 * @author cbeer
 */
public class LDPathTransform implements Transformation<List<Map<String, Collection<Object>>>>  {

    public static final String CONFIGURATION_FOLDER = "/fedora:system/fedora:transform/fedora:ldpath/";

    private static final Comparator<NodeType> BY_NAME =
            (final NodeType o1, final NodeType o2) -> o1.getName().compareTo(o2.getName());

    // TODO: this mime type was made up
    public static final String APPLICATION_RDF_LDPATH = "application/rdf+ldpath";
    private final InputStream query;

    private static final Logger LOGGER = getLogger(LDPathTransform.class);

    /**
     * Construct a new Transform from the InputStream
     * @param query the query
     */
    public LDPathTransform(final InputStream query) {
        this.query = query;
    }

    /**
     * Pull a node-type specific transform out of JCR
     * @param node the node
     * @param key the key
     * @return node-type specific transform
     * @throws RepositoryException if repository exception occurred
     */
    public static LDPathTransform getNodeTypeTransform(final Node node,
        final String key) throws RepositoryException {

        final Node programNode = node.getSession().getNode(CONFIGURATION_FOLDER + key);

        LOGGER.debug("Found program node: {}", programNode.getPath());

        final NodeType primaryNodeType = node.getPrimaryNodeType();

        final Set<NodeType> supertypes = orderedBy(BY_NAME).add(primaryNodeType.getSupertypes()).build();
        final Set<NodeType> mixinTypes = orderedBy(BY_NAME).add(node.getMixinNodeTypes()).build();

        // start with mixins, primary type, and supertypes of primary type
        final ImmutableList.Builder<NodeType> nodeTypesB = builder();
        nodeTypesB.addAll(mixinTypes).add(primaryNodeType).addAll(supertypes);

        // add supertypes of mixins
        mixinTypes.stream().map(mixin -> orderedBy(BY_NAME).add(mixin.getDeclaredSupertypes()).build())
            .forEach(nodeTypesB::addAll);

        final List<NodeType> nodeTypes = nodeTypesB.build();

        LOGGER.debug("Discovered node types: {}", nodeTypes);
        for (final NodeType nodeType : nodeTypes) {
            if (programNode.hasNode(nodeType.toString())) {
                return new LDPathTransform(programNode.getNode(nodeType.toString())
                                               .getNode(JCR_CONTENT)
                                               .getProperty(JCR_DATA)
                                               .getBinary().getStream());
            }
        }

        throw new WebApplicationException(new Exception(
                "Couldn't find transformation for " + node.getPath()
                        + " and transformation key " + key), SC_BAD_REQUEST);
    }

    @Override
    public List<Map<String, Collection<Object>>> apply(final RdfStream stream) {
        final LDPath<RDFNode> ldpathForResource =
            getLdpathResource(stream);

        final Resource context = createResource(stream.topic().getURI());

        try {
            return ImmutableList.of(unsafeCast(
                ldpathForResource.programQuery(context, new InputStreamReader(query))));
        } catch (final LDPathParseException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <F, T> T unsafeCast(final F from) {
        return (T) from;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof LDPathTransform && ((LDPathTransform) other).query.equals(query);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(query);
    }

    /**
     * Get the LDPath resource for an object
     * @param rdfStream
     * @return the LDPath resource for the given object
     */
    private static LDPath<RDFNode> getLdpathResource(final RdfStream rdfStream) {

        return new LDPath<>(new GenericJenaBackend(rdfStream.asModel()));

    }
}
