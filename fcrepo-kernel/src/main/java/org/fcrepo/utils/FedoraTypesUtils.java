
package org.fcrepo.utils;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.modeshape.common.SystemFailureException;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class FedoraTypesUtils {

    static public Predicate<Node> isFedoraObject = new Predicate<Node>() {

        @Override
        public boolean apply(Node node) {

            try {
                return map(node.getMixinNodeTypes(), nodetype2name).contains(
                        FEDORA_OBJECT);
            } catch (RepositoryException e) {
                throw new SystemFailureException(e);
            }
        }
    };
    
    static public Predicate<Node> isFedoraDatastream = new Predicate<Node>() {

        @Override
        public boolean apply(Node node) {

            try {
                return map(node.getMixinNodeTypes(), nodetype2name).contains(
                        FEDORA_DATASTREAM);
            } catch (RepositoryException e) {
                throw new SystemFailureException(e);
            }
        }
    };

    static public Function<NodeType, String> nodetype2name =
            new Function<NodeType, String>() {

                @Override
                public String apply(NodeType t) {
                    return t.getName();
                }
            };

    private static <From, To> Collection<To> map(From[] input,
            Function<From, To> f) {
        return transform(copyOf(input), f);
    }
}
