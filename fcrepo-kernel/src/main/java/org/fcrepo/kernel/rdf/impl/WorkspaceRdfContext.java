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
package org.fcrepo.kernel.rdf.impl;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_DEFAULT_WORKSPACE;
import static org.fcrepo.kernel.RdfLexicon.HAS_WORKSPACE;
import static org.fcrepo.kernel.RdfLexicon.WORKSPACE_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Function;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.functions.GetDefaultWorkspace;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * Constructs RDF from the structure of {@link Workspace}s in the repository.
 *
 * @author ajs6f
 * @since Nov 24, 2013
 *
 * @author cbeer
 * @since 12 Dec 2013
 */
public class WorkspaceRdfContext extends RdfStream {

    private static final Logger LOGGER = getLogger(WorkspaceRdfContext.class);

    private Function<Repository, String> getDefaultWorkspace = new GetDefaultWorkspace();
    /**
     * @param session
     * @param uriInfo
     * @throws RepositoryException
     */
    public WorkspaceRdfContext(final Session session, final IdentifierTranslator subjects)
        throws RepositoryException {
        super();

        final String[] workspaces =
            session.getWorkspace().getAccessibleWorkspaceNames();

        final String defaultWorkspace = getDefaultWorkspace.apply(session.getRepository());
        final Node repositorySubject = subjects.getSubject("/").asNode();

        for (final String workspace : workspaces) {
            final Node resource = subjects.getSubject(
                        "/workspace:" + workspace).asNode();
            LOGGER.debug("Discovered workspace: {}", resource);

            concat(Triple.create(resource, type.asNode(), WORKSPACE_TYPE
                    .asNode()));

            concat(Triple.create(resource, DC_TITLE.asNode(), createLiteral(workspace)));


            concat(Triple.create(repositorySubject, HAS_WORKSPACE.asNode(), resource));

            if (defaultWorkspace.equals(workspace)) {
                concat(Triple.create(repositorySubject, HAS_DEFAULT_WORKSPACE.asNode(), resource));
            }


        }
    }

}
