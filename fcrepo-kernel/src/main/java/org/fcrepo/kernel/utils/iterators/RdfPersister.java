
package org.fcrepo.kernel.utils.iterators;

import java.util.Iterator;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.graph.Triple;

/**
 * Consumes an {@link Iterator} of {@link Triple}s by persisting them
 * into the JCR.
 *
 * @author ajs6f
 * @date Oct 24, 2013
 */
public class RdfPersister implements IteratorConsumer<Triple, Boolean> {

    @Override
    public void consume(final Iterator<Triple> i) {
        // TODO persist RDF into JCR

    }

    @Override
    public ListenableFuture<Boolean> consumeAsync(final Iterator<Triple> i) {
        // TODO persist RDF into JCR and return a ListenableFuture indicating
        // success
        return null;
    }

}
