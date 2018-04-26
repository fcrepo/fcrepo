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

package org.fcrepo.http.commons.responses;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.sparql.core.Quad;

/**
 * @author Daniel Bernstein
 * @since Mar 22, 2017
 */
public class SynchonizedStreamRDFWrapper extends StreamRDFWrapper {

    /**
     *
     * @param stream the StreamRDF
     */
    public SynchonizedStreamRDFWrapper(final StreamRDF stream) {
        super(stream);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.jena.riot.system.StreamRDFWrapper#start()
     */
    @Override
    public synchronized void start() {
        super.start();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.jena.riot.system.StreamRDFWrapper#finish()
     */
    @Override
    public synchronized void finish() {
        super.finish();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.jena.riot.system.StreamRDFWrapper#triple(org.apache.jena.graph.Triple)
     */
    @Override
    public synchronized void triple(final Triple triple) {
        super.triple(triple);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.jena.riot.system.StreamRDFWrapper#prefix(java.lang.String, java.lang.String)
     */
    @Override
    public synchronized void prefix(final String prefix, final String iri) {
        super.prefix(prefix, iri);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.jena.riot.system.StreamRDFWrapper#quad(org.apache.jena.sparql.core.Quad)
     */
    @Override
    public synchronized void quad(final Quad quad) {
        super.quad(quad);
    }

}