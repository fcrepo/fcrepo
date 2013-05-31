/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static com.google.common.base.Preconditions.checkArgument;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.jcr.api.NamespaceRegistry;
import com.google.common.base.Function;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date May 13, 2013
 */
public abstract class NamespaceTools {

    /**
     * @todo Add Documentation.
     */
    public static NamespaceRegistry getNamespaceRegistry(final Session session)
        throws RepositoryException {
        return (NamespaceRegistry) session.getWorkspace().getNamespaceRegistry();
    }

    /**
     * @todo Add Documentation.
     */
    public static NamespaceRegistry getNamespaceRegistry(final Item item)
        throws RepositoryException {
        return getNamespaceRegistry(item.getSession());
    }

    /**
     * We need the Modeshape NamespaceRegistry, because it allows us to register
     * anonymous namespaces.
     * @return
     * @throws RepositoryException
     */
    public static Function<Node, NamespaceRegistry> getNamespaceRegistry = new Function<Node, NamespaceRegistry>() {
        @Override
        public NamespaceRegistry apply(final Node n) {
            try {
                checkArgument(n != null,
                              "null has no Namespace Registry associated " +
                              "with it!");
                return (org.modeshape.jcr.api.NamespaceRegistry)n.getSession().getWorkspace().getNamespaceRegistry();
            } catch (final RepositoryException e) {
                throw new IllegalStateException(e);
            }
        }

    };
}
