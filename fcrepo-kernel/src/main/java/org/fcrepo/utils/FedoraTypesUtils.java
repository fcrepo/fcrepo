
package org.fcrepo.utils;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Convenience class with methods for manipulating Fedora types in the JCR.
 * 
 * @author ajs6f
 *
 */
public class FedoraTypesUtils {

    /**
     * Predicate for determining whether this node is a Fedora object.
     */
    static public Predicate<Node> isFedoraObject = new Predicate<Node>() {

        @Override
        public boolean apply(Node node) {

            try {
                return map(node.getMixinNodeTypes(), nodetype2name).contains(
                        FEDORA_OBJECT);
            } catch (RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Predicate for determining whether this node is a Fedora datastream.
     */
    static public Predicate<Node> isFedoraDatastream = new Predicate<Node>() {

        @Override
        public boolean apply(Node node) {

            try {
                return map(node.getMixinNodeTypes(), nodetype2name).contains(
                        FEDORA_DATASTREAM);
            } catch (RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Translates a node type to its name. 
     */
    static public Function<NodeType, String> nodetype2name =
            new Function<NodeType, String>() {

                @Override
                public String apply(NodeType t) {
                    return t.getName();
                }
            };

    /**
     * Translates a JCR value to its string expression. 
     */
    public static Function<Value, String> value2string =
            new Function<Value, String>() {

                @Override
                public String apply(Value v) {
                    try {
                        return v.getString();
                    } catch (RepositoryException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };

    /**
     * Convenience method for transforming collections into 
     * immutable sets through a mapping function.
     * 
     * @param input A Collection<F>.
     * @param f A Function<F,T>.
     * @return An ImmutableSet copy of input after transformation by f
     */
    public static <From, To> Collection<To> map(From[] input,
            Function<From, To> f) {
        return transform(copyOf(input), f);
    }

}
