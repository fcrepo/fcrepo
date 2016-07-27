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
package org.fcrepo.kernel.api;

import static java.util.Arrays.asList;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;

import org.junit.Test;

/**
 * @author acoburn
 */
public class RdfCollectorsTest {

    @Test
    public void streamToModel() {
        final Node subject = createURI("subject");
        final Node prop1 = createURI("prop1");
        final Node prop2 = createURI("prop2");
        final List<Triple> triples = asList(
                new Triple(subject, prop1, createURI("obj1")),
                new Triple(subject, prop1, createURI("obj2")),
                new Triple(subject, prop1, createURI("obj3")),
                new Triple(subject, prop2, createURI("obj1")),
                new Triple(subject, prop2, createURI("obj2")),
                new Triple(subject, prop2, createURI("obj3")));

        final Model filtered = triples.stream().filter(x -> x.getPredicate().equals(prop1))
                .collect(RdfCollectors.toModel());

        assertEquals(3, filtered.size());
    }
}

