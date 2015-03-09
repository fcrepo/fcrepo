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

package org.fcrepo.kernel.impl.rdf;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Skolemizes blank nodes in a statement.
 *
 * @author ajs6f
 */
public class Skolemizer implements Function<Statement, Statement> {

    private final RDFVisitor nodeSkolemizer;

    private final Model model;

    public static final Resource SKOLEM_TYPE = createResource(REPOSITORY_NAMESPACE + "skolem");

    private final Set<Resource> skolemNodes = new HashSet<>();

    private static final Logger log = getLogger(Skolemizer.class);

    /**
     * @param topic The URI of the contextual topic of the RDF from which this statement was drawn. Blank nodes will
     *        be skolemized to identifiers rooted at this URI.
     */
    public Skolemizer(final Resource topic) {
        this.model = topic.getModel() == null ? createDefaultModel() : topic.getModel();
        // TODO use Java 8's StringJoiner facility.
        final String prefix = topic + "/" + randomUUID().toString().replace('-', '/');
        this.nodeSkolemizer = new NodeSkolemizer(prefix, model);
    }

    @Override
    public Statement apply(final Statement stmnt) {
        log.debug("Skolemizing: {}", stmnt);
        final Resource s = stmnt.getSubject();
        final Resource subject = (Resource) s.visitWith(nodeSkolemizer);
        if (!s.equals(subject)) {
            skolemNodes.add(subject);
        }
        final RDFNode o = stmnt.getObject();
        final RDFNode object = (RDFNode) o.visitWith(nodeSkolemizer);
        if (o.isResource() && !o.equals(object)) {
            skolemNodes.add(object.asResource());
        }
        // predicates are never anonymous
        final Property predicate = stmnt.getPredicate();
        final Statement skolemized = model.createStatement(subject, predicate, object);
        log.debug("to: {}", skolemized);
        return skolemized;
    }

    /**
     * @return Any Skolem nodes that might need to be created.
     */
    public Set<Resource> skolemNodes() {
        return skolemNodes;
    }

    /**
     * Does nothing to literals or URIs, skolemizes bnodes.
     *
     * @author ajs6f
     */
    private static class NodeSkolemizer implements RDFVisitor {

        private final String prefix;

        private final Model model;

        public NodeSkolemizer(final String prefix, final Model model) {
            this.prefix = prefix;
            this.model = model;
        }

        @Override
        public Resource visitBlank(final Resource r, final AnonId id) {
            final Resource skolemNode = model.createResource(prefix + "/" + id.getLabelString().replace(':', '-'));
            // ensures that this skolem node is recognizable as such.
            model.add(model.createStatement(skolemNode, type, SKOLEM_TYPE));
            return skolemNode;
        }

        @Override
        public Resource visitURI(final Resource r, final String uri) {
            return r;
        }

        @Override
        public Literal visitLiteral(final Literal l) {
            return l;
        }
    }
}
