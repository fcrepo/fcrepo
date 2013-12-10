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

package org.fcrepo.transform.transformations;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.backend.jena.GenericJenaBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.fcrepo.transform.Transformation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.transformValues;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.rdf.SerializationUtils.getDatasetSubject;
import static org.fcrepo.kernel.rdf.SerializationUtils.unifyDatasetModel;

/**
 * Utilities for working with LDPath
 */
public class LDPathTransform implements Transformation  {

    public static final String CONFIGURATION_FOLDER = "/fedora:system/fedora:transform/fedora:ldpath/";


    // TODO: this mime type was made up
    public static final String APPLICATION_RDF_LDPATH = "application/rdf+ldpath";
    private final InputStream query;

    /**
     * Construct a new Transform from the InputStream
     * @param query
     */
    public LDPathTransform(final InputStream query) {
        this.query = query;
    }

    /**
     * Pull a node-type specific transform out of JCR
     * @param node
     * @param key
     * @return
     * @throws RepositoryException
     */
    public static LDPathTransform getNodeTypeTransform(final Node node,
        final String key) throws RepositoryException {

        final Node programNode = node.getSession().getNode(CONFIGURATION_FOLDER + key);

        final NodeType primaryNodeType = node.getPrimaryNodeType();

        final NodeType[] supertypes = primaryNodeType.getSupertypes();

        final Iterable<NodeType> nodeTypes =
            concat(of(primaryNodeType), copyOf(supertypes));

        for (final NodeType nodeType : nodeTypes) {
            if (programNode.hasNode(nodeType.toString())) {
                return new LDPathTransform(programNode.getNode(nodeType.toString())
                                               .getNode("jcr:content")
                                               .getProperty("jcr:data")
                                               .getBinary().getStream());
            }
        }

        return null;
    }

    @Override
    public List<Map<String, Collection<Object>>> apply(final Dataset dataset) {
        try {
            final LDPath<RDFNode> ldpathForResource =
                getLdpathResource(dataset);

            final Resource context =
                createResource(getDatasetSubject(dataset).getURI());

            final Map<String, Collection<?>> wildcardCollection =
                ldpathForResource.programQuery(context, new InputStreamReader(
                        query));

            return ImmutableList.of(transformLdpathOutputToSomethingSerializable(wildcardCollection));
        } catch (LDPathParseException | RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getQuery() {
        return query;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof LDPathTransform &&
                   query.equals(((LDPathTransform)other).getQuery());
    }

    /**
     * Get the LDPath resource for an object
     * @param dataset
     * @return
     * @throws RepositoryException
     */
    private LDPath<RDFNode> getLdpathResource(final Dataset dataset) throws RepositoryException {

        final Model model = unifyDatasetModel(dataset);

        final GenericJenaBackend genericJenaBackend = new GenericJenaBackend(model);

        final LDPath<RDFNode> ldpath = new LDPath<RDFNode>(genericJenaBackend);

        return ldpath;
    }

    /**
     * In order for the JAX-RS serialization magic to work, we have to turn the map into
     * a non-wildcard type.
     * @param collectionMap
     * @return
     */
    private Map<String, Collection<Object>> transformLdpathOutputToSomethingSerializable(
        final Map<String, Collection<?>> collectionMap) {

        return transformValues(collectionMap,
                WILDCARD_COLLECTION_TO_OBJECT_COLLECTION);
    }

    private static final Function<Collection<?>, Collection<Object>> WILDCARD_COLLECTION_TO_OBJECT_COLLECTION =
        new Function<Collection<?>, Collection<Object>>() {

            @Override
            public Collection<Object> apply(final Collection<?> input) {
                return transform(input, ANYTHING_TO_OBJECT_FUNCTION);
            }
        };

    private static final Function<Object, Object> ANYTHING_TO_OBJECT_FUNCTION =
        new Function<Object, Object>() {

            @Override
            public Object apply(final Object input) {
                return input;
            }
        };
}
