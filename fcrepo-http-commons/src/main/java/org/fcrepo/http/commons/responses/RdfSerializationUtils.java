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
package org.fcrepo.http.commons.responses;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import org.slf4j.Logger;

/**
 * Utilities to help with serializing a graph to an HTTP resource
 *
 * @author awoods
 */
public class RdfSerializationUtils {

    private static final Logger LOGGER = getLogger(RdfSerializationUtils.class);

    /**
     * No public constructor on utility class
     */
    private RdfSerializationUtils() {
    }

    /**
     * The RDF predicate that will indicate the primary node type.
     */
    public static Node primaryTypePredicate =
            createURI(getRDFNamespaceForJcrNamespace(JCR_NAMESPACE) +
                    "primaryType");

    /**
     * The RDF predicate that will indicate the mixin types.
     */
    public static Node mixinTypesPredicate =
        createURI(getRDFNamespaceForJcrNamespace(JCR_NAMESPACE) +
                  "mixinTypes");

    private static final Function<RDFNode, String> stringConverter = new Function<RDFNode, String>() {
        @Override
        public String apply(final RDFNode node) {
            return node.asLiteral().getLexicalForm();
        }
    };

    /**
     * Get the very first value for a predicate as a string, or null if the
     * predicate is not used
     *
     * @param rdf
     * @param subject
     * @param predicate
     * @return first value for the given predicate or null if not found
     */
    public static String getFirstValueForPredicate(final Model rdf,
            final Node subject, final Node predicate) {
        final NodeIterator statements = rdf.listObjectsOfProperty(createResource(subject.getURI()),
                createProperty(predicate.getURI()));
        // we'll take the first one we get
        if (statements.hasNext()) {
            return statements.next().asLiteral().getLexicalForm();
        }
        LOGGER.trace("No value found for predicate: {}", predicate);
        return null;
    }

    /**
     * Get all the values for a predicate as a string array, or null if the
     * predicate is not used
     *
     * @param rdf
     * @param subject
     * @param predicate
     * @return all values for the given predicate
     */
    public static Iterator<String> getAllValuesForPredicate(final Model rdf,
            final Node subject, final Node predicate) {
        final NodeIterator objects =
            rdf.listObjectsOfProperty(createResource(subject.getURI()),
                createProperty(predicate.getURI()));

        final ImmutableList<RDFNode> copy = copyOf(objects);
        return transform(copy, stringConverter).iterator();
    }

}
