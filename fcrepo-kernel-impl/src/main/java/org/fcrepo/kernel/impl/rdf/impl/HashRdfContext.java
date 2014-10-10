package org.fcrepo.kernel.impl.rdf.impl;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.utils.iterators.NodeIterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author cabeer
 * @since 10/9/14
 */
public class HashRdfContext extends NodeRdfContext {


    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws javax.jcr.RepositoryException
     */
    public HashRdfContext(final Node node, final IdentifierConverter<Resource, Node> graphSubjects)
            throws RepositoryException {
        super(node, graphSubjects);


        concat(Iterators.concat(Iterators.transform(new NodeIterator(node().getNodes("#*")), new Function<Node, Iterator<Triple>>() {
            @Override
            public Iterator<Triple> apply(final Node input) {
                try {
                    return new PropertiesRdfContext(node, graphSubjects);
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        })));
    }
}
