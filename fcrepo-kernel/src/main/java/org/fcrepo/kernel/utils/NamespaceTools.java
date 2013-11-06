/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.utils;

import static com.google.common.base.Preconditions.checkArgument;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.NamespaceRegistry;

import com.google.common.base.Function;

/**
 * Tools for working with the JCR Namespace Registry
 * (wrapping some non-standard Modeshape machinery)
 * @author Benjamin Armintor
 * @date May 13, 2013
 */
public abstract class NamespaceTools {

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
