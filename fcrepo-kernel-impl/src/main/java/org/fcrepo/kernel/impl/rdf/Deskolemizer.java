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

import static com.google.common.cache.CacheBuilder.newBuilder;
import static com.google.common.cache.CacheLoader.from;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.models.FedoraResource;

import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.cache.LoadingCache;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Deskolemization is abstractly a function from RDF nodes to RDF nodes, but here we implement it, purely for
 * convenience of operation, as a function from triples to triples. This object should be used to translate only one
 * document's scope of RDF.
 *
 * @author ajs6f
 */
public class Deskolemizer implements Function<Triple, Triple> {

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;

    private final Model context;

    /**
     * A map from which to select blank nodes as replacements for Skolem nodes.
     */
    private final LoadingCache<Resource, Resource> bnodeSubstitutions = newBuilder().build(from(
            new Supplier<Resource>() {

                @Override
                public Resource get() {
                    return context.createResource();
                }
            }));

    private static final Logger log = getLogger(Deskolemizer.class);

    /**
     * @param idTranslator
     * @param context
     */
    public Deskolemizer(final IdentifierConverter<Resource, FedoraResource> idTranslator, final Model model) {
        this.idTranslator = idTranslator;
        this.context = model == null ? createDefaultModel() : model;
    }

    @Override
    public Triple apply(final Triple t) {
        log.debug("Deskolemizing: {}", t);
        final Statement stmnt = context.asStatement(t);
        final Resource s = stmnt.getSubject();
        final RDFNode o = stmnt.getObject();
        try {
            final Resource subject = deskolemize(s).asResource();
            final RDFNode object = deskolemize(o);
            // predicates cannot be blank nodes in RDF 1.1
            final Triple deskolemized = context.createStatement(subject, stmnt.getPredicate(), object).asTriple();
            log.debug("Deskolemized to {}", deskolemized);
            return deskolemized;
        } catch (final RuntimeException e) {
            log.error("Received exception while deskolemizing:", e);
            throw e;
        }
    }

    /**
     * The node-to-node deskolemization function.
     *
     * @param n
     * @return a deskolemized version of n
     */
    private RDFNode deskolemize(final RDFNode n) {
        log.debug("Deskolemizing RDF node: {}", n);
        if (isSkolem(n)) {
            log.debug("Replacing {} with bnode.", n);
            return bnodeSubstitutions.getUnchecked(n.asResource());
        }
        return n;
    }

    private boolean isSkolem(final RDFNode n) {
        return n.isURIResource() &&
                (n.asResource().getURI().indexOf('?') == -1) &&
                idTranslator.inDomain(n.asResource()) &&
                !idTranslator.asString(n.asResource()).contains("/fcr:") &&
                idTranslator.convert(n.asResource()).hasType("fedora:Skolem");
    }
}