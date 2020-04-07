/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.models;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;

import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;


/**
 * Implementation of an LDP Container resource
 *
 * @author bbpennel
 */
public class ContainerImpl extends FedoraResourceImpl implements Container {

    /**
     * Construct the container
     *
     * @param fedoraID internal identifier
     * @param tx transaction
     * @param pSessionManager session manager
     * @param resourceFactory resource factory
     */
    public ContainerImpl(final FedoraId fedoraID, final Transaction tx,
                         final PersistentStorageSessionManager pSessionManager, final ResourceFactory resourceFactory) {
        super(fedoraID, tx, pSessionManager, resourceFactory);
    }

    @Override
    public FedoraResource getDescribedResource() {
        return this;
    }

    @Override
    public RdfStream getTriples() {
        final Node subject = createURI(getId());
        final Stream<Triple> extra_triples = Stream.of(
                Triple.create(subject, type.asNode(), RDF_SOURCE.asNode()),
                Triple.create(subject, type.asNode(), CONTAINER.asNode()),
                Triple.create(subject, type.asNode(), FEDORA_CONTAINER.asNode()),
                Triple.create(subject, type.asNode(), FEDORA_RESOURCE.asNode())
        );
        return new DefaultRdfStream(createURI(getId()), Stream.concat(super.getTriples(), extra_triples));
    }
}
