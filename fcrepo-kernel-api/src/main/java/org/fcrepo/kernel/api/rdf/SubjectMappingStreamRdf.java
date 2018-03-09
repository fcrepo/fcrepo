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
package org.fcrepo.kernel.api.rdf;

import static org.apache.jena.graph.Factory.createDefaultGraph;
import static org.apache.jena.rdf.model.ModelFactory.createModelForGraph;

import java.net.URI;

import static org.apache.jena.graph.NodeFactory.createURI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.lang.SinkTriplesToGraph;
import org.apache.jena.riot.system.StreamRDFBase;

/**
 * StreamRDF implementation that maps URIs to a specified destination URI.
 *
 * @author escowles
 * @author bbpennel
 * @since 2018-03-06
 */
public class SubjectMappingStreamRdf extends StreamRDFBase {

    private final String sourceUri;

    private final String destinationUri;

    private final Graph graph;

    private final Sink<Triple> sink;

    /**
     * Create a subject-mapping RDF stream
     *
     * @param sourceUri the source URI to map subjects from
     * @param destinationUri the destination URI to map subjects to
     */
    public SubjectMappingStreamRdf(final URI sourceUri, final URI destinationUri) {
        if (sourceUri == null || destinationUri == null) {
            throw new IllegalArgumentException("sourceUri and destinationUri are required");
        }
        this.sourceUri = sourceUri.toString();
        this.destinationUri = destinationUri.toString();
        this.graph = createDefaultGraph();
        this.sink = new SinkTriplesToGraph(true, graph);
    }

    @Override
    public void triple(final Triple t) {
        sink.send(Triple.create(remap(t.getSubject()), t.getPredicate(), t.getObject()));
    }

    private Node remap(final Node node) {
        if (node.isURI() && node.getURI().equals(sourceUri)) {
            return createURI(node.getURI().replaceFirst(sourceUri, destinationUri));
        }

        return node;
    }

    @Override
    public void finish() {
        sink.flush();
        sink.close();
    }

    /**
     * Get the mapped triples as a model
     *
     * @return A model representing the triples sent to this stream
     */
    public Model getModel() {
        return createModelForGraph(graph);
    }
}
