/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static javax.jcr.query.Query.JCR_SQL2;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.utils.JcrRdfTools.getRDFNamespaceForJcrNamespace;

import java.io.InputStream;
import java.util.Collection;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.modeshape.jcr.JcrValueFactory;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.Namespaced;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Convenience class with static methods for manipulating Fedora types in the
 * JCR.
 *
 * @author ajs6f
 * @date Feb 14, 2013
 */
public abstract class FedoraTypesUtils {
    /**
     * Predicate for determining whether this {@link Node} is a Fedora object.
     */
    public static Predicate<Node> isFedoraResource = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkArgument(node != null, "null cannot be a Fedora object!");
            try {
                return map(node.getMixinNodeTypes(),
                           nodetype2name).contains(FEDORA_RESOURCE);
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Predicate for determining whether this {@link Node} is a Fedora object.
     */
    public static Predicate<Node> isFedoraObject = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkArgument(node != null, "null cannot be a Fedora object!");
            try {
                return map(node.getMixinNodeTypes(),
                           nodetype2name).contains(FEDORA_OBJECT);
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Predicate for determining whether this {@link Node} is a Fedora
     * datastream.
     */
    public static Predicate<Node> isFedoraDatastream = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkArgument(node != null, "null cannot be a Fedora datastream!");
            try {
                return map(node.getMixinNodeTypes(),
                           nodetype2name).contains(FEDORA_DATASTREAM);
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Translates a {@link NodeType} to its {@link String} name.
     */
    public static Function<NodeType, String> nodetype2name = new Function<NodeType, String>() {
        @Override
        public String apply(final NodeType t) {
            checkArgument(t != null, "null has no name!");
            return t.getName();
        }
    };

    /**
     * Translates a JCR {@link Value} to its {@link String} expression.
     */
    public static Function<Value, String> value2string = new Function<Value, String>() {
        @Override
        public String apply(final Value v) {
            try {
                checkArgument(v != null,
                              "null has no appropriate " +
                              "String representation!");
                return v.getString();
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Check if a JCR property is a multivalued property or not
     */
    public static Predicate<Property> isMultipleValuedProperty = new Predicate<Property>() {
        @Override
        public boolean apply(final Property p) {
            checkArgument(p != null,
                          "null is neither multiple or not multiple!");
            try {
                return p.isMultiple();
            } catch (final RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    };


    /**
     * Check if a node is "internal" and should not be exposed e.g. via the
     * REST API
     */
    public static Predicate<Node> isInternalNode = new Predicate<Node>() {
        @Override
        public boolean apply(final Node n) {
            checkArgument(n != null,
                          "null is neither multiple or not multiple!");
            try {
                final NodeType primaryNodeType = n.getPrimaryNodeType();
                return primaryNodeType != null &&
                    primaryNodeType.isNodeType("mode:system");
            } catch (final RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    };


    /**
     * Retrieves a JCR {@link ValueFactory} for use with a {@ link Node}
     */
    public static Function<Node, ValueFactory> getValueFactory = new Function<Node, ValueFactory>() {
        @Override
        public ValueFactory apply(final Node n) {
            try {
                checkArgument(n != null,
                              "null has no ValueFactory associated with it!");
                return n.getSession().getValueFactory();
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Map a JCR property to an RDF property with the right namespace URI and
     * local name
     */
    public static Function<Property, com.hp.hpl.jena.rdf.model.Property> getPredicateForProperty = new Function<Property,com.hp.hpl.jena.rdf.model.Property>() {
        @Override
        public com.hp.hpl.jena.rdf.model.Property
        apply(final Property property) {
            final Namespaced nsProperty = (Namespaced)property;
            try {
                final String uri = nsProperty.getNamespaceURI();
                return ResourceFactory
                    .createProperty(getRDFNamespaceForJcrNamespace(uri),
                                    nsProperty.getLocalName());
            } catch (RepositoryException e) {
                throw new IllegalStateException(e);
            }

        }
    };

    /**
     * ISODateTimeFormat is thread-safe and immutable, and the formatters it
     * returns are as well.
     */
    private static final DateTimeFormatter FMT = ISODateTimeFormat.dateTime();


    /**
     * Creates a JCR {@link Binary}
     *
     * @param n a {@link Node}
     * @param i an {@link InputStream}
     * @return a JCR {@link Binary}
     */
    public static Binary getBinary(final Node n, final InputStream i) {
        try {
            checkArgument(n != null,
                          "null cannot have a Binary created for it!");
            checkArgument(i != null,
                          "null cannot have a Binary created from it!");
            return n.getSession().getValueFactory().createBinary(i);
        } catch (final RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a JCR {@link Binary}
     *
     * @param n a {@link Node}
     * @param i an {@link InputStream}
     * @return a JCR {@link Binary}
     */
    public static Binary getBinary(final Node n, final InputStream i,
                                   final String hint) {
        try {
            checkArgument(n != null,
                          "null cannot have a Binary created for it!");
            checkArgument(i != null,
                          "null cannot have a Binary created from it!");
            JcrValueFactory jcrValueFactory =
                ((JcrValueFactory)n.getSession().getValueFactory());
            return jcrValueFactory.createBinary(i, hint);
        } catch (final RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Get the JCR Node Type manager
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static NodeTypeManager getNodeTypeManager(final Node node)
        throws RepositoryException {
        return node.getSession().getWorkspace().getNodeTypeManager();
    }

    /**
     * Get the property definition information (containing type and multi-value
     * information)
     * @param node the node to use for inferring the property definition
     * @param propertyName the property name to retrieve a definition for
     * @return a JCR PropertyDefinition, if available, or null
     * @throws javax.jcr.RepositoryException
     */
    public static PropertyDefinition getDefinitionForPropertyName(final Node node, final String propertyName)
        throws RepositoryException {
        final PropertyDefinition[] propertyDefinitions =
            getNodeTypeManager(node).getNodeType(FEDORA_RESOURCE).getPropertyDefinitions();

        for (PropertyDefinition p : propertyDefinitions) {
            if (p.getName().equals(propertyName)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Convenience method for transforming arrays into
     * {@link Collection}s through a mapping {@link Function}.
     *
     * @param input A Collection<F>.
     * @param f A Function<F,T>.
     * @return An ImmutableSet copy of input after transformation by f
     */
    public static <F, T> Collection<T> map(final F[] input,
                                           final Function<F, T> f) {
        return transform(copyOf(input), f);
    }

    /**
     * @param date Instance of java.util.Date.
     * @return the lexical form of the XSD dateTime value, e.g.
     *         "2006-11-13T09:40:55.001Z".
     */
    public static String convertDateToXSDString(final long date) {
        final DateTime dt = new DateTime(date, DateTimeZone.UTC);
        return FMT.print(dt);
    }

    /**
     * Get the JCR Base version for a node
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static Version getBaseVersion(final Node node)
        throws RepositoryException {
        return node.getSession()
            .getWorkspace().getVersionManager().getBaseVersion(node.getPath());
    }

    /**
     * Get the JCR VersionHistory for an existing node
     * @param node
     * @return
     * @throws RepositoryException
     */

    public static VersionHistory getVersionHistory(final Node node)
        throws RepositoryException {
        return getVersionHistory(node.getSession(), node.getPath());
    }

    /**
     * Get the JCR VersionHistory for a node at a given JCR path
     * @param session
     * @param path
     * @return
     * @throws RepositoryException
     */
    public static VersionHistory getVersionHistory(final Session session,
                                                   final String path)
        throws RepositoryException {
        return session.getWorkspace().getVersionManager()
            .getVersionHistory(path);
    }

    /**
     *
     * @return a double of the size of the fedora:datastream binary content
     * @throws RepositoryException
     */
    public static long getRepositoryCount(final Repository repository)
        throws RepositoryException {
        final Session session = repository.login();
        try {
            final QueryManager queryManager =
                session.getWorkspace().getQueryManager();

            final String querystring =
                "SELECT [" + JcrConstants.JCR_PATH +
                "] FROM [" + FedoraJcrTypes.FEDORA_OBJECT + "]";

            final QueryResult queryResults =
                queryManager.createQuery(querystring, JCR_SQL2).execute();

            return queryResults.getRows().getSize();
        } finally {

            session.logout();
        }
    }

    /**
     *
     * @return a double of the size of the fedora:datastream binary content
     * @throws RepositoryException
     */
    public static long getRepositorySize(final Repository repository)
        throws RepositoryException {
        final Session session = repository.login();
        long sum = 0;
        final QueryManager queryManager =
            session.getWorkspace().getQueryManager();

        final String querystring =
            "SELECT [" + FedoraJcrTypes.CONTENT_SIZE +
            "] FROM [" + FedoraJcrTypes.FEDORA_BINARY + "]";

        final QueryResult queryResults =
            queryManager.createQuery(querystring, JCR_SQL2).execute();

        for (final RowIterator rows = queryResults.getRows(); rows.hasNext();) {
            final Value value =
                rows.nextRow().getValue(FedoraJcrTypes.CONTENT_SIZE);
            sum += value.getLong();
        }

        session.logout();

        return sum;
    }
}
