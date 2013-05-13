
package org.fcrepo.generator.rdf;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.FedoraObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultFedoraObjectGenerator implements TripleSource<FedoraObject> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultFedoraObjectGenerator.class);

    @Override
    public List<org.fcrepo.generator.rdf.TripleSource.Triple> getTriples(
            final FedoraObject obj, final UriInfo... uriInfos)
            throws RepositoryException {
        final Node node = obj.getNode();
        LOGGER.debug("Generating triples for object: " + obj.getName());
        // now we alter the subjects of these triples to be true 
        // (and dereferenceable) URIs, based on our knowledge that these are
        // Fedora objects
        return DefaultNodeGenerator.getTriples(node, uriInfos);

    }
}
