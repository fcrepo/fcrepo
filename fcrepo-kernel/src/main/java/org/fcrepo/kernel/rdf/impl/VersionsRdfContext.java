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

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_LABEL;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.kernel.utils.iterators.VersionIterator;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;


/**
 * An {@link NodeRdfContext} that supplies {@link Triple}s concerning
 * the versions of a selected {@link Node}.
 *
 * @author ajs6f
 * @date Oct 15, 2013
 */
public class VersionsRdfContext extends RdfStream {

    private final VersionManager versionManager;

    private final VersionHistory versionHistory;

    private final GraphSubjects graphSubjects;

    private final LowLevelStorageService lowLevelStorageService;

    private final com.hp.hpl.jena.graph.Node subject;

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
        super();
        this.lowLevelStorageService = lowLevelStorageService;
        this.graphSubjects = graphSubjects;
        this.subject = graphSubjects.getGraphSubject(node).asNode();
        versionManager = node.getSession().getWorkspace().getVersionManager();
        versionHistory = versionManager.getVersionHistory(node.getPath());

        concat(versionTriples());
    }

    private Iterator<Triple> versionTriples() throws RepositoryException {
        return Iterators.concat(Iterators.transform(new VersionIterator(versionHistory
                .getAllVersions()), version2triples));
    }

    private Function<Version, Iterator<Triple>> version2triples =
        new Function<Version, Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final Version version) {

                try {
                    final Node frozenNode = version.getFrozenNode();
                    final com.hp.hpl.jena.graph.Node versionSubject =
                        graphSubjects.getGraphSubject(frozenNode).asNode();

                    final RdfStream results =
                            new RdfStream(new PropertiesRdfContext(frozenNode,
                                    graphSubjects, lowLevelStorageService)
                                    .iterator());

                    results.concat(create(subject, HAS_VERSION.asNode(),
                            versionSubject));

                    for (final String label : versionHistory
                            .getVersionLabels(version)) {
                        results.concat(create(versionSubject, HAS_VERSION_LABEL
                                .asNode(), createLiteral(label)));
                    }

                    return results;

                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }

        };


}
