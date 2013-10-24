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

import static org.fcrepo.kernel.utils.NodePropertiesTools.appendOrReplaceNodeProperty;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Consumes an {@link RdfStream} by adding its contents to the
 * JCR.
 *
 * @see {@link RdfRemover} for contrast
 * @author ajs6f
 * @date Oct 24, 2013
 */
public class RdfAdder extends RdfPersister {

    private static final Logger LOGGER = getLogger(RdfAdder.class);

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public RdfAdder(final GraphSubjects graphSubjects, final Session session,
        final RdfStream stream) {
        super(graphSubjects, session, stream);
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource,
        final Node subjectNode) throws RepositoryException {
        final String namespacePrefix;
        try {
            namespacePrefix =
                session.getNamespacePrefix(mixinResource.getNameSpace());
        } catch (final NamespaceException e) {
            throw new MalformedRdfException(
                    "Unable to resolve registered namespace for resource "
                            + mixinResource.toString());
        }
        final String mixinName =
            namespacePrefix + ":" + mixinResource.getLocalName();
        if (session.getWorkspace().getNodeTypeManager().hasNodeType(mixinName)) {
            if (subjectNode.canAddMixin(mixinName)) {
                LOGGER.debug("Adding mixin: {} to node: {}.", mixinName,
                        subjectNode.getPath());
                subjectNode.addMixin(mixinName);
            } else {
                throw new MalformedRdfException(
                        "Could not persist triple containing type assertion:"
                                + mixinResource.toString()
                                + " because no such mixin/type exists in repository!");
            }
        }
    }


    @Override
    protected void operateOnOneValueOfProperty(final Node n, final String p,
        final Value v) throws RepositoryException {
        LOGGER.debug("Adding property: {} with value: {} to node: {}.", p, v, n
                .getPath());
        appendOrReplaceNodeProperty(n, p, v);
    }
}
