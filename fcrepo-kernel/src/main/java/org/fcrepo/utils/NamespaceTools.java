package org.fcrepo.utils;

import static com.google.common.base.Preconditions.checkArgument;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Function;

public abstract class NamespaceTools {
    
    public static org.modeshape.jcr.api.NamespaceRegistry getNamespaceRegistry(final Session session)
            throws RepositoryException {
        return (org.modeshape.jcr.api.NamespaceRegistry)session.getWorkspace().getNamespaceRegistry();
    }
    
    public static org.modeshape.jcr.api.NamespaceRegistry getNamespaceRegistry(final Item item)
            throws RepositoryException {
        return getNamespaceRegistry(item.getSession());
    }

    /**
     * We need the Modeshape NamespaceRegistry, because it allows us to register anonymous namespaces.
     * @return
     * @throws RepositoryException
     */
    public static Function<Node, org.modeshape.jcr.api.NamespaceRegistry> getNamespaceRegistry =
            new Function<Node, org.modeshape.jcr.api.NamespaceRegistry>() {
                @Override
                public org.modeshape.jcr.api.NamespaceRegistry apply(final Node n) {
                    try {
                        checkArgument(n != null,
                                             "null has no Namespace Registry associated with it!");
                        return (org.modeshape.jcr.api.NamespaceRegistry)n.getSession().getWorkspace().getNamespaceRegistry();
                    } catch (final RepositoryException e) {
                        throw new IllegalStateException(e);
                    }
                }
    
            };
}
