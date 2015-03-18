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

import static com.google.common.hash.Hashing.murmur3_32;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createStatement;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.hash.HashCode;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Skolemization is abstractly a function from RDF nodes to RDF nodes, but here we implement it, purely for
 * convenience of operation, as a function from triples to triples. {@link #nodeSkolemizer} represents the
 * node-to-node function. This class will make two blank nodes with the same identifier into the same skolem resource
 * because it uses only one prefix for its lifetime and because the mapping from a given blank node to a skolem
 * resource depends only on that prefix and on a MurmurHash3 of the identifier of the blank node in question. An
 * instance of this class should be used only with a contextual topic of one single resource and only for one
 * document's scope of RDF about that resource.
 *
 * @author ajs6f
 */
public class Skolemizer implements Function<Statement, Statement>, Supplier<Set<Resource>> {

    private final RDFVisitor nodeSkolemizer;

    private final Set<Resource> skolemNodes = new HashSet<>();

    private static final Logger log = getLogger(Skolemizer.class);

    /**
     * @param topic The URI of the contextual topic of the RDF from which this statement was drawn. Blank nodes will
     *        be skolemized to identifiers rooted at this URI.
     */
    public Skolemizer(final Resource topic) {
        // TODO use Java 8's StringJoiner facility.
        final String prefix = topic + "/" + randomUUID().toString().replace('-', '/');
        this.nodeSkolemizer = new NodeSkolemizer(prefix);
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
        // predicates are never anonymous in RDF 1.1
        final Property predicate = stmnt.getPredicate();
        final Statement skolemized = createStatement(subject, predicate, object);
        log.debug("to: {}", skolemized);
        return skolemized;
    }

    /**
     * @return Any Skolem nodes that have been generated and might need to be persisted.
     */
    @Override
    public Set<Resource> get() {
        return skolemNodes;
    }

    /**
     * Does nothing to literals or URIs, but skolemizes blank nodes.
     *
     * @author ajs6f
     */
    private static class NodeSkolemizer implements RDFVisitor {

        private final String prefix;

        public NodeSkolemizer(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Resource visitBlank(final Resource r, final AnonId id) {
            final HashCode suffix = murmur3_32().hashBytes(id.getLabelString().getBytes());
            return createResource(prefix + "/" + suffix);
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
