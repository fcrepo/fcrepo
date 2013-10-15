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

import static com.google.common.collect.ImmutableSet.builder;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_LABEL;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.NodeRdfContext;
import org.fcrepo.kernel.services.LowLevelStorageService;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.graph.Triple;


/**
 * An {@link NodeRdfContext} that supplies {@link Triple}s concerning
 * the versions of a selected {@link Node}.
 *
 * @author ajs6f
 * @date Oct 15, 2013
 */
public class VersionsRdfContext extends NodeRdfContext {

    /**
     * Ordinary constructor.
     *
     * @param node
     * @param graphSubjects
     * @param lowLevelStorageService
     * @throws RepositoryException
     */
    public VersionsRdfContext(final Node node, final GraphSubjects graphSubjects,
        final LowLevelStorageService lowLevelStorageService)
        throws RepositoryException {
        super(node, graphSubjects, lowLevelStorageService);
        context().concat(versionTriples());
    }

    private Iterator<Triple> versionTriples() throws RepositoryException {
        final VersionHistory versionHistory =
            node().getSession().getWorkspace().getVersionManager()
                    .getVersionHistory(node().getPath());

        final VersionIterator versionIterator = versionHistory.getAllVersions();
        final ImmutableSet.Builder<Triple> b = builder();
        while (versionIterator.hasNext()) {
            final Version version = versionIterator.nextVersion();
            final Node frozenNode = version.getFrozenNode();
            final com.hp.hpl.jena.graph.Node versionSubject =
                graphSubjects().getGraphSubject(frozenNode).asNode();

            b.add(create(subject(), HAS_VERSION.asNode(), versionSubject));

            final String[] versionLabels =
                versionHistory.getVersionLabels(version);
            for (final String label : versionLabels) {
                b.add(create(versionSubject, HAS_VERSION_LABEL.asNode(), createLiteral(label)));
            }
            context().concat(
                    new PropertiesRdfContext(frozenNode, graphSubjects(),
                            lowLevelStorageService()).context());

        }
        return b.build().iterator();
    }


}
