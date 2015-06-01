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
package org.fcrepo.kernel.impl.utils.iterators;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.impl.rdf.ManagedRdf.isManagedMixin;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.base.Joiner;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.ConstraintViolationException;
import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.exception.ServerManagedTypeException;
import org.fcrepo.kernel.exception.OutOfDomainSubjectException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.kernel.utils.iterators.RdfStreamConsumer;
import org.slf4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ajs6f
 * @since Oct 24, 2013
 */
public abstract class PersistingRdfStreamConsumer implements RdfStreamConsumer {

    private final RdfStream stream;

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;

    // if it's not about a Fedora resource, we don't care.
    protected final Predicate<Triple> isFedoraSubjectTriple;

    private final JcrRdfTools jcrRdfTools;

    private static final Model m = createDefaultModel();

    private static final Logger LOGGER = getLogger(PersistingRdfStreamConsumer.class);

    private final List<String> exceptions;

    /**
     * Ordinary constructor.
     *
     * @param idTranslator the id translator
     * @param session the session
     * @param stream the rdf stream
     */
    public PersistingRdfStreamConsumer(final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final Session session, final RdfStream stream) {
        this.idTranslator = idTranslator;
        this.jcrRdfTools = new JcrRdfTools(idTranslator, session);
        this.isFedoraSubjectTriple = new Predicate<Triple>() {

            @Override
            public boolean apply(final Triple t) {

                final boolean result = idTranslator.inDomain(m.asStatement(t).getSubject())
                        || t.getSubject().isBlank();
                if (result) {
                    LOGGER.debug(
                            "Discovered a Fedora-relevant subject in triple: {}.",
                            t);
                } else {
                    LOGGER.error("subject ({}) is not in repository domain.", t.getSubject().toString());
                    throw new OutOfDomainSubjectException(t.getSubject().toString());
                }
                return result;
            }

        };
        // we fail on non-Fedora RDF
        this.stream =
                stream.withThisContext(stream.filter(isFedoraSubjectTriple));

        this.exceptions = new ArrayList<>();
    }

    @Override
    public void consume() throws MalformedRdfException {
        while (stream.hasNext()) {
            final Statement t = m.asStatement(stream.next());
            LOGGER.debug("Operating triple {}.", t);

            try {
                operateOnTriple(t);
            } catch (final ConstraintViolationException e) {
                throw e;
            } catch (final MalformedRdfException e) {
                exceptions.add(e.getMessage());
            }
        }

        if (!exceptions.isEmpty()) {
            throw new MalformedRdfException(Joiner.on("\n").join(exceptions));
        }
    }

    protected void operateOnTriple(final Statement input) throws MalformedRdfException {
        try {

            final Statement t = jcrRdfTools.skolemize(idTranslator, input);

            final Resource subject = t.getSubject();
            final FedoraResource subjectNode = translator().convert(subject);

            // if this is a user-managed RDF type assertion, update the node's
            // mixins. If it isn't, treat it as a "data" property.
            if (t.getPredicate().equals(type) && t.getObject().isResource()) {
                final Resource mixinResource = t.getObject().asResource();
                if (!isManagedMixin.apply(mixinResource)) {
                    LOGGER.debug("Operating on node: {} with mixin: {}.",
                            subjectNode, mixinResource);
                    operateOnMixin(mixinResource, subjectNode);
                } else {
                    LOGGER.debug("Found repository-managed mixin on which we will not operate.");
                    throw new ServerManagedTypeException(String.format(
                            "The repository type (%s) of this resource is system managed.", mixinResource));
                }
            } else {
                LOGGER.debug("Operating on node: {} from triple: {}.", subjectNode,
                        t);
                operateOnProperty(t, subjectNode);
            }
        } catch (final ConstraintViolationException e) {
            throw e;
        } catch (final RepositoryException | RepositoryRuntimeException e) {
            throw new MalformedRdfException(e.getMessage(), e);
        }
    }

    protected abstract void operateOnProperty(final Statement t,
        final FedoraResource subjectNode) throws RepositoryException;

    protected abstract void operateOnMixin(final Resource mixinResource,
        final FedoraResource subjectNode) throws RepositoryException;

    @Override
    public ListenableFuture<Boolean> consumeAsync() {
        // TODO make this actually asynch
        final SettableFuture<Boolean> result = SettableFuture.create();
        try {
            consume();
            result.set(true);
        } catch (final MalformedRdfException e) {
            LOGGER.warn("Got exception consuming RDF stream", e);
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
    public IdentifierConverter<Resource, FedoraResource> translator() {
        return idTranslator;
    }

    /**
     * @return the jcrRdfTools
     */
    public JcrRdfTools jcrRdfTools() {
        return jcrRdfTools;
    }

}
