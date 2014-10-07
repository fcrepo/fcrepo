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
package org.fcrepo.kernel.impl.rdf.impl;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_LABEL;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.kernel.utils.iterators.VersionIterator;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import org.slf4j.Logger;


/**
 * An {@link NodeRdfContext} that supplies {@link Triple}s concerning
 * the versions of a selected {@link Node}.
 *
 * @author ajs6f
 * @since Oct 15, 2013
 */
public class VersionsRdfContext extends RdfStream {

    private final VersionManager versionManager;

    private final VersionHistory versionHistory;

    private final IdentifierTranslator graphSubjects;

    private final com.hp.hpl.jena.graph.Node subject;

    private final Logger LOGGER = getLogger(VersionsRdfContext.class);

    /**
     * Ordinary constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws RepositoryException
     */
    public VersionsRdfContext(final Node node, final IdentifierTranslator graphSubjects)
        throws RepositoryException {
        super();
        this.graphSubjects = graphSubjects;
        this.subject = graphSubjects.getSubject(node.getPath()).asNode();
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
                    /* Discard jcr:rootVersion (unless we have tests in the codebase that rely on it):*/
                    if (version.getName().equals(versionHistory.getRootVersion().getName())
                      && versionHistory.getAllVersions().getSize() != 1) {
                        LOGGER.trace("Skipped root version from triples");
                        return new RdfStream();
                    }

                    final Node frozenNode = version.getFrozenNode();
                    final com.hp.hpl.jena.graph.Node versionSubject =
                        graphSubjects.getSubject(frozenNode.getPath()).asNode();

                    final RdfStream results =
                            new RdfStream(new PropertiesRdfContext(frozenNode,
                                    graphSubjects));

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
