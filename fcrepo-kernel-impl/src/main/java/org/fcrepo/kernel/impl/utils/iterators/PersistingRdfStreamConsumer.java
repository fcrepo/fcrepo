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
package org.fcrepo.kernel.impl.utils.iterators;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.impl.rdf.ManagedRdf.isManagedMixin;
import static org.fcrepo.kernel.impl.rdf.ManagedRdf.isManagedTriple;
import static org.slf4j.LoggerFactory.getLogger;
import org.fcrepo.kernel.impl.utils.NodePropertiesTools;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.hp.hpl.jena.rdf.model.AnonId;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.kernel.utils.iterators.RdfStreamConsumer;
import org.modeshape.jcr.api.JcrTools;
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

import java.util.HashMap;
import java.util.Map;

/**
 * @author ajs6f
 * @since Oct 24, 2013
 */
public abstract class PersistingRdfStreamConsumer implements RdfStreamConsumer {

    private final RdfStream stream;

    private final IdentifierTranslator idTranslator;

    private final Session session;

    // if it's not about a Fedora resource, we don't care.
    protected final Predicate<Triple> isFedoraSubjectTriple;

    private final Map<AnonId, Resource> skolemizedBnodeMap;

    private final JcrRdfTools jcrRdfTools;

    private static final JcrTools jcrTools = new JcrTools();

    private static final Model m = createDefaultModel();

    private static final Logger LOGGER = getLogger(PersistingRdfStreamConsumer.class);

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public PersistingRdfStreamConsumer(final IdentifierTranslator graphSubjects,
            final Session session, final RdfStream stream) {
        this.idTranslator = graphSubjects;
        this.jcrRdfTools = JcrRdfTools.withContext(graphSubjects, session);
        this.isFedoraSubjectTriple = new Predicate<Triple>() {

            @Override
            public boolean apply(final Triple t) {

                final boolean result =
                        graphSubjects.isFedoraGraphSubject(m.asStatement(t).getSubject()) || t.getSubject().isBlank();
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
        this.skolemizedBnodeMap = new HashMap<>();
        // we knock out managed RDF and non-Fedora RDF
        this.stream =
                stream.withThisContext(stream.filter(and(not(isManagedTriple),
                        isFedoraSubjectTriple)));
        this.session = session;
    }

    @Override
    public void consume() throws RepositoryException {
        while (stream.hasNext()) {
            Statement t = m.asStatement(stream.next());
            LOGGER.debug("Operating triple {}.", t);

            if (t.getObject().isAnon()) {
                t = t.changeObject(getSkolemizedResource(t.getObject()));
            }

            if (t.getSubject().isAnon()) {
                t = m.createStatement(getSkolemizedResource(t.getSubject()), t.getPredicate(), t.getObject());
            }

            LOGGER.trace("Operating on skolemized triple {}.", t);

            operateOnTriple(t);
        }

    }

    private Resource getSkolemizedResource(final RDFNode resource) throws RepositoryException {
        final AnonId id = resource.asResource().getId();

        if (!skolemizedBnodeMap.containsKey(id)) {
            final Node orCreateNode =
                    jcrTools.findOrCreateNode(session, "/.well-known/genid/" + randomUUID().toString());
            orCreateNode.addMixin("fedora:blanknode");
            final Resource skolemizedSubject = idTranslator().getSubject(orCreateNode.getPath());
            skolemizedBnodeMap.put(id, skolemizedSubject);
        }

        return skolemizedBnodeMap.get(id);
    }

    protected void operateOnTriple(final Statement t)
        throws RepositoryException {
        final Resource subject = t.getSubject();
        final Node subjectNode = session().getNode(idTranslator().getPathFromSubject(subject));

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
            }
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

    protected String getPropertyNameFromPredicate(final Node subjectNode,
                                                  final Resource predicate) throws RepositoryException {
        return jcrRdfTools().getPropertyNameFromPredicate(subjectNode, predicate, stream.namespaces());
    }

    protected Value createValue(final Node subjectNode, final RDFNode object,
        final Integer propertyType) throws RepositoryException {
        return jcrRdfTools().createValue(subjectNode, object, propertyType);
    }


    protected Value createValue(final Node n, final Statement t, final String propertyName) throws RepositoryException {
        final NodePropertiesTools propertiesTools = new NodePropertiesTools();
        return jcrRdfTools().createValue(n, t.getObject(), propertiesTools.getPropertyType(n, propertyName));
    }

    protected boolean sessionHasType(final Session session, final String mixinName) throws RepositoryException {
        if (session == null) {
            return false;
        }
        return session().getWorkspace().getNodeTypeManager().hasNodeType(mixinName);
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
    public IdentifierTranslator idTranslator() {
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
