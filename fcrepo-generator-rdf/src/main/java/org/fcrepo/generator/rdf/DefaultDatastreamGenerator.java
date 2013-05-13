
package org.fcrepo.generator.rdf;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.Datastream;
import org.slf4j.Logger;

public class DefaultDatastreamGenerator implements TripleSource<Datastream> {

    private static final Logger LOGGER =
            getLogger(DefaultDatastreamGenerator.class);

    @Override
    public List<Triple> getTriples(final Datastream ds,
            final UriInfo... uriInfos) throws RepositoryException {
        final Node node = ds.getNode();
        LOGGER.debug("Generating triples for object: " + ds.getDsId());
        // now we alter the subjects of these triples to be true 
        // (and dereferenceable) URIs, based on our knowledge that these are
        // Fedora datastreams
        return DefaultNodeGenerator.getTriples(node, uriInfos);

    }
}
