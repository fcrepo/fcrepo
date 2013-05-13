
package org.fcrepo.generator.rdf;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.transform;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.generator.rdf.TripleSource.Triple;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import com.google.common.base.Function;

public abstract class DefaultNodeGenerator {

    private static final PropertiesGenerator propertiesGenerator =
            new PropertiesGenerator();

    private static final NodeTypesGenerator nodeTypesGenerator =
            new NodeTypesGenerator();

    private static final Logger logger =
            getLogger(DefaultNodeGenerator.class);

    public static List<TripleSource.Triple> getTriples(final Node node,
            final UriInfo... uriInfos) throws RepositoryException {
        final UriInfo uriInfo = uriInfos[0];
        logger.debug("Generating triples for object: {}" + node.getPath());
        // now we alter the subjects of these triples to be true 
        // (and dereferenceable) URIs, based on our knowledge that these are
        // Fedora datastreams
        return transform(copyOf(concat(propertiesGenerator.getTriples(node,
                uriInfo), nodeTypesGenerator.getTriples(node))),
                new Function<Triple, Triple>() {

                    @Override
                    public Triple apply(final Triple t) {
                        if (t == null) {
                            return null;
                        }
                        try {
                            return new Triple(uriInfo.getBaseUriBuilder().path(
                                    "rest").path(node.getPath())
                                    .build().toString(), t.predicate, t.object);
                        } catch (UriBuilderException | RepositoryException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                });

    }
}
