/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.utils;

import com.google.common.base.Predicate;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.services.functions.AnyTypesPredicate;
import org.fcrepo.kernel.services.functions.JcrPropertyFunctions;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.impl.utils.NodePropertiesTools.REFERENCE_PROPERTY_SUFFIX;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Convenience class with static methods for manipulating Fedora types in the
 * JCR.
 *
 * @author ajs6f
 * @since Feb 14, 2013
 */
public abstract class FedoraTypesUtils implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(FedoraTypesUtils.class);

    /**
     * Predicate for determining whether this {@link Node} is a Fedora object.
     */
    public static Predicate<Node> isFedoraObject =
            new AnyTypesPredicate(FEDORA_OBJECT);

    /**
     * Predicate for determining whether this {@link Node} is a Fedora
     * datastream.
     */
    public static Predicate<Node> isFedoraDatastream =
            new AnyTypesPredicate(FEDORA_DATASTREAM);


    /**
     * Predicate for determining whether this {@link Node} is a Fedora
     * binary.
     */
    public static Predicate<Node> isFedoraBinary =
            new AnyTypesPredicate(FEDORA_BINARY);

    /**
     * Predicate for determining whether this {@link FedoraResource} has a frozen node
     */
    public static Predicate<FedoraResource> isFrozenNode =
            new Predicate<FedoraResource>() {

                @Override
                public boolean apply(final FedoraResource f) {
                    return f.hasType(FROZEN_NODE) || f.getPath().contains(JCR_FROZEN_NODE);
                }
     };

    /**
     * Predicate for determining whether this {@link Node} is a Fedora
     * binary.
     */
    public static Predicate<Node> isBlankNode =
            new AnyTypesPredicate(FEDORA_BLANKNODE);

    /**
     * Check if a property is a reference property.
     */
    public static Predicate<Property> isReferenceProperty =
        new Predicate<Property>() {

            @Override
            public boolean apply(final Property p) {
                try {
                    return (p.getType() == REFERENCE || p.getType() == WEAKREFERENCE)
                        && p.getName().endsWith(REFERENCE_PROPERTY_SUFFIX);
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        };

    /**
    * Check whether a property is an internal property that should be suppressed
    * from external output.
    */
    public static Predicate<Property> isInternalProperty =
        new Predicate<Property>() {

            @Override
            public boolean apply(final Property p) {
                return isReferenceProperty.apply(p) || JcrPropertyFunctions.isBinaryContentProperty.apply(p)
                        || isProtectedAndShouldBeHidden.apply(p);
            }
        };

    /**
     * Check whether a property is protected (ie, cannot be modified directly) but
     * is not one we've explicitly chosen to include.
     */
    public static Predicate<Property> isProtectedAndShouldBeHidden =
        new Predicate<Property>() {

            @Override
            public boolean apply(final Property p) {
                try {
                    if (!p.getDefinition().isProtected()) {
                        return false;
                    } else if (p.getParent().isNodeType(FROZEN_NODE)) {
                        // everything on a frozen node is protected
                        // but we wish to display it anyway and there's
                        // another mechanism in place to make clear that
                        // things cannot be edited.
                        return false;
                    } else {
                        final String name = p.getName();
                        for (String exposedName : EXPOSED_PROTECTED_JCR_TYPES) {
                            if (name.equals(exposedName)) {
                                return false;
                            }
                        }
                        return true;
                    }
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        };

    /**
     * Check if a node is "internal" and should not be exposed e.g. via the REST
     * API
     */
    public static Predicate<Node> isInternalNode = new Predicate<Node>() {

        @Override
        public boolean apply(final Node n) {
            checkNotNull(n, "null is neither internal nor not internal!");
            try {
                return n.isNodeType("mode:system");
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
    };

    /**
     * Get the closest ancestor that current exists
     *
     * @param session
     * @param path
     * @return
     * @throws RepositoryException
     */
    public static Node getClosestExistingAncestor(final Session session,
                                                  final String path) throws RepositoryException {
        final String[] pathSegments = path.replaceAll("^/+", "").replaceAll("/+$", "").split("/");

        Node node = session.getRootNode();

        final int len = pathSegments.length;
        for (int i = 0; i != len; ++i) {
            final String pathSegment = pathSegments[i];

            if (node.hasNode(pathSegment)) {
                // Find the existing node ...
                node = node.getNode(pathSegment);
            } else {
                return node;
            }

        }

        return node;
    }

}
