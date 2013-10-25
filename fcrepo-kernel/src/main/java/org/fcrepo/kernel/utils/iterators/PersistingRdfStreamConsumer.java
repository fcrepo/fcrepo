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

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterators.filter;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.utils.iterators.UnmanagedRdfStream.isManagedTriple;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.utils.JcrRdfTools;
import org.slf4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author ajs6f
 * @date Oct 24, 2013
 */
public abstract class PersistingRdfStreamConsumer implements RdfStreamConsumer {

    private final RdfStream stream;

    private final GraphSubjects idTranslator;

    private final Session session;

    private final JcrRdfTools jcrRdfTools;

    // if it's not about a Fedora resource, we don't care.
    protected final Predicate<Triple> isFedoraSubjectTriple;

    private static final Model m = createDefaultModel();

    private static final Logger LOGGER = getLogger(PersistingRdfStreamConsumer.class);

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public PersistingRdfStreamConsumer(final GraphSubjects graphSubjects,
            final Session session, final RdfStream stream) {
        // this filters out triples with internal or managed predicates
        this.idTranslator = graphSubjects;
        this.isFedoraSubjectTriple = new Predicate<Triple>() {

            @Override
            public boolean apply(final Triple t) {

                final boolean result =
                    graphSubjects.isFedoraGraphSubject(m.asStatement(t)
                            .getSubject());
                if (result) {
                    LOGGER.debug(
                            "Discovered a Fedora-relevant subject in triple: {}.",
                            t);
                } else {
                    LOGGER.debug("Ignoring triple: {}.", t);

                }
                return result;
            }

        };
        // we knock out managed RDF and non-Fedora RDF
        this.stream =
            new RdfStream(filter(stream, and(not(isManagedTriple),
                    isFedoraSubjectTriple)));
        this.session = session;
        this.jcrRdfTools = JcrRdfTools.withContext(graphSubjects, session);
    }

    @Override
    public void consume() throws Exception {
        while (stream.hasNext()) {
            final Statement t = m.asStatement(stream.next());
            LOGGER.debug("Operating on triple {}.", t);
            operateOnTriple(t);
        }

    }

    protected void operateOnTriple(final Statement t)
        throws RepositoryException {
        final Resource subject = t.getSubject();
        final Node subjectNode = idTranslator().getNodeFromGraphSubject(subject);

        // if this is a RDF type assertion, update the node's mixins. If it
        // isn't, treat it as a "data" property.
        if (t.getPredicate().equals(type) && t.getObject().isResource()) {
            final Resource mixinResource = t.getObject().asResource();
            LOGGER.debug("Operating on node: {} with mixin: {}.", subjectNode,
                    mixinResource);
            operateOnMixin(mixinResource, subjectNode);
        } else {
            LOGGER.debug("Operating on node: {} from triple: {}.", subjectNode,
                    t);
            operateOnProperty(t, subjectNode);
        }
    }

    protected String getPropertyNameFromPredicate(final Node subjectNode,
        final Property predicate) throws RepositoryException {
        return jcrRdfTools().getPropertyNameFromPredicate(subjectNode, predicate,
                stream.namespaces());
    }

    protected Value createValue(final Node subjectNode, final RDFNode object,
        final Integer propertyType) throws RepositoryException {
        return jcrRdfTools().createValue(subjectNode, object, propertyType);
    }

    protected abstract void operateOnProperty(final Statement t,
        final Node subjectNode) throws RepositoryException;

    protected abstract void operateOnMixin(final Resource mixinResource,
        final Node subjectNode) throws RepositoryException;

    @Override
    public ListenableFuture<Boolean> consumeAsync() {
        // TODO make this actually asynch
        final SettableFuture<Boolean> result = SettableFuture.create();
        try {
            consume();
            result.set(true);
        } catch (final Exception e) {
            result.setException(e);
            result.set(false);
        }
        return result;
    }


    /**
     * @return the stream
     */
    public RdfStream stream() {
        return stream;
    }


    /**
     * @return the idTranslator
     */
    public GraphSubjects idTranslator() {
        return idTranslator;
    }


    /**
     * @return the session
     */
    public Session session() {
        return session;
    }


    /**
     * @return the jcrRdfTools
     */
    public JcrRdfTools jcrRdfTools() {
        return jcrRdfTools;
    }
}
