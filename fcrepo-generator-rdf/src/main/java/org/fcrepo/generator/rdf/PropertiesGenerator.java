
package org.fcrepo.generator.rdf;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableList.builder;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;

import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList.Builder;

public class PropertiesGenerator implements TripleSource<Node> {

    final private Logger logger = LoggerFactory
            .getLogger(PropertiesGenerator.class);

    @Override
    public List<Triple> getTriples(final Node source, final UriInfo... uriInfo)
            throws RepositoryException {
        logger.trace("Entering getTriples()...");
        logger.debug("Generating triples for node: " + source.getPath());
        final Builder<Triple> triples = builder();

        @SuppressWarnings("unchecked")
        final List<Property> properties = copyOf(source.getProperties());
        logger.debug("Retrieved properties: " + properties.toString());

        triples.addAll(transform(filter(properties, not(isMultiple)),
                singlevaluedprop2triple));
        triples.addAll(concat(transform(filter(properties, isMultiple),
                multivaluedprop2triples)));

        logger.trace("Leaving getTriples().");
        return triples.build();
    }

    private static Function<Property, Triple> singlevaluedprop2triple =
            new Function<Property, Triple>() {

                @Override
                public Triple apply(Property p) {
                    try {
                        return new Triple(
                                p.getParent().getName(),
                                expandJCRNamespace(p.getName(), p.getSession()
                                        .getWorkspace().getNamespaceRegistry()),
                                p.getString());
                    } catch (RepositoryException e) {
                        throw new IllegalStateException(e);
                    }
                }

            };

    private static Function<Property, List<Triple>> multivaluedprop2triples =
            new Function<Property, List<Triple>>() {

                @Override
                public List<Triple> apply(Property p) {
                    final Builder<Triple> triples = builder();
                    try {
                        for (Value v : p.getValues()) {
                            triples.add(new Triple(p.getParent().getName(),
                                    expandJCRNamespace(p.getName(), p
                                            .getSession().getWorkspace()
                                            .getNamespaceRegistry()), v
                                            .getString()));
                        }
                        return triples.build();
                    } catch (RepositoryException e) {
                        throw new IllegalStateException(e);
                    }
                }

            };

    private static String expandJCRNamespace(final String name,
            NamespaceRegistry nReg) throws RepositoryException {
        final String predicatePrefix = name.substring(0, name.indexOf(":"));
        return name.replaceFirst(predicatePrefix + ":", nReg
                .getURI(predicatePrefix));
    }

    private static Predicate<Property> isMultiple = new Predicate<Property>() {

        @Override
        public boolean apply(Property p) {
            try {
                return p.isMultiple();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    };

}
