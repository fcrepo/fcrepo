
package org.fcrepo.generator.rdf;

import static com.google.common.collect.ImmutableList.builder;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList.Builder;

public class NodeTypesGenerator implements TripleSource<Node> {

    final public static String TYPE_PREDICATE =
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    final private Logger logger = LoggerFactory
            .getLogger(NodeTypesGenerator.class);

    @Override
    public List<org.fcrepo.generator.rdf.TripleSource.Triple> getTriples(
            final Node source, final UriInfo... uriInfo)
            throws RepositoryException {
        logger.trace("Entering getTriples()...");
        final Builder<Triple> triples = builder();
        logger.debug("Generating triples for node: " + source.getPath());
        final String subject = source.getPath();

        // add primary NodeType
        final Triple pt =
                new Triple(subject, TYPE_PREDICATE, source.getPrimaryNodeType()
                        .getName());
        triples.add(pt);
        logger.debug("Added primary type triple: " + pt.toString());

        // add mixin NodeTypes

        for (final NodeType type : source.getMixinNodeTypes()) {
            final Triple t =
                    new Triple(subject, TYPE_PREDICATE, type.getName());
            triples.add(t);
            logger.debug("Added mixin type triple: " + pt.toString());

        }
        logger.trace("Leaving getTriples().");
        return triples.build();
    }

}
