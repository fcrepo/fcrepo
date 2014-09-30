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

import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Consumes an {@link org.fcrepo.kernel.utils.iterators.RdfStream} by removing its contents from the
 * JCR.
 *
 * @see RdfAdder
 * @author ajs6f
 * @since Oct 24, 2013
 */
public class RdfRemover extends PersistingRdfStreamConsumer {

    private static final Logger LOGGER = getLogger(RdfRemover.class);

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public RdfRemover(final IdentifierTranslator graphSubjects, final Session session,
        final RdfStream stream) {
        super(graphSubjects, session, stream);
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource,
        final Node subjectNode) throws RepositoryException {

        jcrRdfTools().removeMixin(subjectNode, mixinResource, stream().namespaces());
    }

    @Override
    protected void operateOnProperty(final Statement t, final Node n)
        throws RepositoryException {
        LOGGER.debug("Trying to remove property from triple: {} on node: {}.", t, n
                .getPath());
        jcrRdfTools().removeProperty(n, t.getPredicate(), t.getObject(), stream().namespaces());

    }
}
