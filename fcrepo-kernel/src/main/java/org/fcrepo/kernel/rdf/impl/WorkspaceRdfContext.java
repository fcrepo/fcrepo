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

package org.fcrepo.kernel.rdf.impl;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.RdfLexicon.NOT_IMPLEMENTED;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * Constructs RDF from the structure of {@link Workspace}s in the repository.
 *
 * @author ajs6f
 * @date Nov 24, 2013
 */
public class WorkspaceRdfContext extends RdfStream {

    private static Logger LOGGER = getLogger(WorkspaceRdfContext.class);

    /**
     * @param session
     * @param uriInfo
     * @throws RepositoryException
     */
    public WorkspaceRdfContext(final Session session, final UriInfo uriInfo)
        throws RepositoryException {

        final String[] workspaces =
            session.getWorkspace().getAccessibleWorkspaceNames();

        for (final String workspace : workspaces) {
            final Node resource =
                createURI(uriInfo.getBaseUriBuilder().path(
                        "/workspace:" + workspace).build().toString());
            LOGGER.debug("Discovered workspace: {}", resource);
            concat(Triple.create(resource, type.asNode(), NOT_IMPLEMENTED
                    .asNode()));
        }
    }

}
