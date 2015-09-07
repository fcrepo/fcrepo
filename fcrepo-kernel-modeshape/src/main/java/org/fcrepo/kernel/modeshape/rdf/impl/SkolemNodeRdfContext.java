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
package org.fcrepo.kernel.modeshape.rdf.impl;

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyValueIterator;
import org.slf4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.utils.UncheckedPredicate.uncheck;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter.nodeForValue;
import static org.fcrepo.kernel.modeshape.rdf.impl.ReferencesRdfContext.REFERENCE_TYPES;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isSkolemNode;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Embed triples describing all skolem nodes in the RDF stream
 *
 * @author cabeer
 * @author ajs6f
 * @since 10/9/14
 */
public class SkolemNodeRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(SkolemNodeRdfContext.class);

    private static final List<Class<? extends NodeRdfContext>> contexts =
            asList(TypeRdfContext.class, PropertiesRdfContext.class, SkolemNodeRdfContext.class);
    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the idTranslator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public SkolemNodeRdfContext(final FedoraResource resource,
                               final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);
        concat(flatMap(getBlankNodesIterator(), n -> nodeConverter.convert(n).getTriples(idTranslator, contexts)));
    }

    @SuppressWarnings("unchecked")
    private Iterator<Node> getBlankNodesIterator() throws RepositoryException {
        final Iterator<Property> properties = resource().getNode().getProperties();

        final Iterator<Property> references = Iterators.filter(properties,
                uncheck((final Property p) -> REFERENCE_TYPES.contains(p.getType()))::test);

        final Iterator<Node> nodes = Iterators.transform(new PropertyValueIterator(references), valueToNode::apply);

        return Iterators.filter(nodes, n -> Objects.nonNull(n) && isSkolemNode.test(n));
    }

    private final Function<Value, Node> valueToNode = v -> {
        try {
            return nodeForValue(session(), v);

        } catch (final AccessDeniedException e) {
            LOGGER.error("Link inaccessible by requesting user: {}, {}", v, session().getUserID());
            return null;

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    };
}
