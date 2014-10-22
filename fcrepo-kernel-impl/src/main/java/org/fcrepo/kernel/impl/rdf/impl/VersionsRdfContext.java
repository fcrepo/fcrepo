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
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
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
 * @since Oct 15, 2013
 */
public class VersionsRdfContext extends RdfStream {

    private final VersionHistory versionHistory;

    private final IdentifierConverter<Resource, FedoraResource> graphSubjects;

    private final com.hp.hpl.jena.graph.Node subject;


    /**
     * Ordinary constructor.
     *
     * @param resource
     * @param graphSubjects
     * @throws RepositoryException
     */
    public VersionsRdfContext(final FedoraResource resource,
                              final IdentifierConverter<Resource, FedoraResource> graphSubjects)
        throws RepositoryException {
        super();
        this.graphSubjects = graphSubjects;
        this.subject = graphSubjects.reverse().convert(resource).asNode();
        versionHistory = resource.getVersionHistory();

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
                    final com.hp.hpl.jena.graph.Node versionSubject
                            = nodeToResource(graphSubjects).convert(frozenNode).asNode();

                    final RdfStream results = new PropertiesRdfContext(nodeConverter.convert(frozenNode),
                            graphSubjects);

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
