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

package org.fcrepo.kernel.impl.rdf.impl;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.NodeIterator;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Iterator;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/25/14
 */
public class LdpContainerRdfContext extends NodeRdfContext {
    private static final Logger LOGGER = getLogger(ChildrenRdfContext.class);

    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws javax.jcr.RepositoryException
     */
    public LdpContainerRdfContext(final Node node, final IdentifierTranslator graphSubjects)
            throws RepositoryException {
        super(node, graphSubjects);
        final PropertyIterator properties = new PropertyIterator(node.getReferences("ldp:membershipResource"));

        if (properties.hasNext()) {
            LOGGER.trace("Found membership containers for {}", node);
            concat(membershipContext(properties));
        }
    }

    private Iterator<Triple> membershipContext(final PropertyIterator properties) {
        return Iterators.concat(Iterators.transform(properties, nodes2triples()));
    }

    private Function<Property, Iterator<Triple>> nodes2triples() {
        return new Function<Property,Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final Property input) {
                try {
                    final Node inputNode = input.getParent();

                    final String memberRelation;

                    if (inputNode.hasProperty("ldp:hasMemberRelation")) {
                        final Property property = inputNode.getProperty("ldp:hasMemberRelation");
                        memberRelation = property.getString();
                    } else {
                        memberRelation = "ldp:member";
                    }

                    if (inputNode.hasNodes()) {
                        final NodeIterator memberNodes = new NodeIterator(inputNode.getNodes());
                        return Iterators.transform(memberNodes, new Function<Node, Triple>() {

                            @Override
                            public Triple apply(final Node child) {
                                try {
                                    final com.hp.hpl.jena.graph.Node childSubject
                                            = graphSubjects().getSubject(child.getPath()).asNode();
                                    return create(subject(), NodeFactory.createURI(memberRelation), childSubject);
                                } catch (final RepositoryException e) {
                                    throw new RepositoryRuntimeException(e);
                                }
                            }
                        });
                    } else {
                        return Collections.emptyIterator();
                    }
                } catch (RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        };
    }

}
