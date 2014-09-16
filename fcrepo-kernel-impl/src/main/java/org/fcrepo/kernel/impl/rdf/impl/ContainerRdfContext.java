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

import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/16/14
 */
public class ContainerRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(ContainerRdfContext.class);

    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws javax.jcr.RepositoryException
     */
    public ContainerRdfContext(final Node node, final IdentifierTranslator graphSubjects) throws RepositoryException {
        super(node, graphSubjects);

        concat(containerContext());

    }

    private Triple[] containerContext() {
        return new Triple[] {
                create(subject(), type.asNode(), CONTAINER.asNode()),
                create(subject(), type.asNode(), DIRECT_CONTAINER.asNode()),
                create(subject(), MEMBERSHIP_RESOURCE.asNode(), subject()),
                create(subject(), HAS_MEMBER_RELATION.asNode(), HAS_CHILD
                        .asNode())};
    }

}
