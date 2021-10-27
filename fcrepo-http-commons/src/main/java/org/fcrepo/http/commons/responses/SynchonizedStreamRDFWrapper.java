/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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