
package org.fcrepo.generator.rdf;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.transform;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.FedoraObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

public class DefaultFedoraObjectGenerator implements TripleSource<FedoraObject> {

    final private static PropertiesGenerator propertiesGenerator =
            new PropertiesGenerator();

    final private static NodeTypesGenerator nodeTypesGenerator =
            new NodeTypesGenerator();

    final private Logger logger = LoggerFactory
            .getLogger(DefaultFedoraObjectGenerator.class);

    @Override
    public List<org.fcrepo.generator.rdf.TripleSource.Triple> getTriples(
            final FedoraObject obj, final UriInfo... uriInfos)
            throws RepositoryException {
        final UriInfo uriInfo = uriInfos[0];
        final Node node = obj.getNode();
        logger.debug("Generating triples for object: " + obj.getName());
        // now we alter the subjects of these triples to be true 
        // (and dereferenceable) URIs, based on our knowledge that these are
        // Fedora objects
        return transform(copyOf(concat(propertiesGenerator.getTriples(node,
                uriInfo), nodeTypesGenerator.getTriples(node))),
                new Function<Triple, Triple>() {

                    @Override
                    public Triple apply(Triple t) {
                        return new Triple(uriInfo.getBaseUriBuilder().path(
                                t.subject).build().toString(), t.predicate,
                                t.object);
                    }
                });

    }
}
