package org.fcrepo.utils;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Function;


public abstract class NamespaceTools {
    
    public static org.modeshape.jcr.api.NamespaceRegistry getNamespaceRegistry(final Session session)
            throws RepositoryException {
        return (org.modeshape.jcr.api.NamespaceRegistry)session.getWorkspace().getNamespaceRegistry();
    }
    
    public static org.modeshape.jcr.api.NamespaceRegistry getNamespaceRegistry(final Node node)
            throws RepositoryException {
        return getNamespaceRegistry(node.getSession());
    }

    public static Map<String, String> getRepositoryNamespaces(final Session session)
            throws RepositoryException {
        final NamespaceRegistry reg = getNamespaceRegistry(session);
                ;
        final String[] prefixes = reg.getPrefixes();
        final HashMap<String, String> result =
                new HashMap<String, String>(prefixes.length);
        for (final String prefix : reg.getPrefixes()) {
            result.put(prefix, reg.getURI(prefix));
        }
        return result;
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
