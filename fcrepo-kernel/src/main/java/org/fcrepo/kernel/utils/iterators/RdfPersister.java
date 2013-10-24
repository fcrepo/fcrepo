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

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.utils.NodePropertiesTools.getPropertyType;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.utils.JcrRdfTools;
import org.slf4j.Logger;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author ajs6f
 * @date Oct 24, 2013
 */
public abstract class RdfPersister implements RdfStreamConsumer {

    protected final RdfStream stream;

    protected final GraphSubjects idTranslator;

    protected final Session session;

    protected final JcrRdfTools jcrRdfTools;

    private static final Model m = createDefaultModel();

    private static final Logger LOGGER = getLogger(RdfPersister.class);

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public RdfPersister(final GraphSubjects graphSubjects,
            final Session session, final RdfStream stream) {
        this.stream = stream;
        this.idTranslator = graphSubjects;
        this.session = session;
        this.jcrRdfTools = JcrRdfTools.withContext(graphSubjects, session);
    }

    @Override
    public void consume() throws Exception {
        while (stream.hasNext()) {
            final Statement t = m.asStatement(stream.next());
            LOGGER.debug("Removing triple {} from repository.", t);
            operateOnTriple(t);
        }

    }

    protected void operateOnTriple(final Statement t)
        throws RepositoryException {
        final Resource subject = t.getSubject();

        // if it's not about a node, we don't care.
        if (!idTranslator.isFedoraGraphSubject(subject)) {
            return;
        }

        final Node subjectNode = idTranslator.getNodeFromGraphSubject(subject);

        // special logic for handling rdf:type updates.
        // if the object is an already-existing mixin, update
        // the node's mixins. If it isn't, just treat it normally.
        if (t.getPredicate().equals(type) && t.getObject().isResource()) {
            final Resource mixinResource = t.getObject().asResource();
            operateOnMixin(mixinResource, subjectNode);
        }
        final String propertyName =
            jcrRdfTools.getPropertyNameFromPredicate(subjectNode, t
                    .getPredicate());

        if (subjectNode.hasProperty(propertyName)) {
            final Value v =
                jcrRdfTools.createValue(subjectNode, t.getObject(),
                        getPropertyType(subjectNode, propertyName));
            operateOnOneValueOfProperty(subjectNode, propertyName, v);
        }
    }

    protected abstract void operateOnOneValueOfProperty(final Node subjectNode,
        final String propertyName, final Value v) throws RepositoryException;

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
}
