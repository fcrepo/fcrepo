
package org.fcrepo.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OWNED;

import java.io.InputStream;
import java.util.Collection;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Convenience class with static methods for manipulating Fedora types in the JCR.
 * 
 * @author ajs6f
 *
 */
public abstract class FedoraTypesUtils {

    /**
     * Predicate for determining whether this {@link Node} is a Fedora object.
     */
    public static Predicate<Node> isFedoraObject = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkArgument(node != null, "null cannot be a Fedora object!");
            try {
                return map(node.getMixinNodeTypes(), nodetype2name).contains(
                        FEDORA_OBJECT);
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Predicate for determining whether this {@link Node} is a Fedora datastream.
     */
    public static Predicate<Node> isFedoraDatastream = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkArgument(node != null, "null cannot be a Fedora datastream!");
            try {
                return map(node.getMixinNodeTypes(), nodetype2name).contains(
                        FEDORA_DATASTREAM);
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Predicate for determining whether this {@link Node} is owned in the Fedora sense.
     */
    public static Predicate<Node> isOwned = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkArgument(node != null, "null cannot be owned by anyone!");
            try {
                return map(node.getMixinNodeTypes(), nodetype2name).contains(
                        FEDORA_OWNED);
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Translates a {@link NodeType} to its {@link String} name. 
     */
    public static Function<NodeType, String> nodetype2name =
            new Function<NodeType, String>() {

                @Override
                public String apply(final NodeType t) {
                    checkArgument(t != null, "null has no name!");
                    return t.getName();
                }
            };

    /**
     * Translates a JCR {@link Value} to its {@link String} expression. 
     */
    public static Function<Value, String> value2string =
            new Function<Value, String>() {

                @Override
                public String apply(final Value v) {
                    try {
                        checkArgument(v != null,
                                "null has no appropriate String representation!");
                        return v.getString();
                    } catch (final RepositoryException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };

    public static Predicate<Property> isMultipleValuedProperty =
            new Predicate<Property>() {

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
     * Retrieves a JCR {@link ValueFactory} for use with a {@ link Node}
     */
    public static Function<Node, ValueFactory> getValueFactory =
            new Function<Node, ValueFactory>() {

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

}
