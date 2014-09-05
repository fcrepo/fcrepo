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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.services.functions.AnyTypesPredicate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionHistory;
import java.util.Collection;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Iterators.contains;
import static com.google.common.collect.Iterators.forArray;
import static com.google.common.collect.Iterators.transform;
import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.impl.utils.NodePropertiesTools.REFERENCE_PROPERTY_SUFFIX;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
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
     * Predicate for determining whether this {@link Node} is a Fedora resource.
     */
    public static Predicate<Node> isFedoraResource = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkNotNull(node, "null cannot be a Fedora object!");
            try {
                return map(node.getMixinNodeTypes(), nodetype2name).contains(
                        FEDORA_RESOURCE);
            } catch (final RepositoryException e) {
                throw propagate(e);
            }
        }
    };

    /**
     * Predicate for determining whether this {@link Node} is a frozen node
     * (a part of the system version history).
     */
    public static Predicate<Node> isFrozen = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkNotNull(node, "null cannot be a Frozen node!");
            try {
                return node.getPrimaryNodeType().getName().equals(FROZEN_NODE);
            } catch (final RepositoryException e) {
                throw propagate(e);
            }
        }
    };

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
     * Predicate for objects, datastreams, whatever!
     */

    public static Predicate<Node> isFedoraObjectOrDatastream =
            new AnyTypesPredicate(FEDORA_OBJECT, FEDORA_DATASTREAM);

    /**
     * Translates a {@link NodeType} to its {@link String} name.
     */
    public static Function<NodeType, String> nodetype2name =
        new Function<NodeType, String>() {

            @Override
            public String apply(final NodeType t) {
                checkNotNull(t, "null has no name!");
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
                    checkNotNull(v, "null has no appropriate "
                                        + "String representation!");
                    return v.getString();
                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }
        };

    /**
     * Constructs an {@link Iterator} of {@link Value}s from any
     * {@link Property}, multi-valued or not.
     */
    public static Function<Property, Iterator<Value>> property2values =
        new Function<Property, Iterator<Value>>() {

            @Override
            public Iterator<Value> apply(final Property p) {
                try {
                    if (p.isMultiple()) {
                        LOGGER.debug("Found multi-valued property: {}", p);
                        return Iterators.forArray(p.getValues());
                    }
                    LOGGER.debug("Found single-valued property: {}", p);
                    return Iterators.forArray(p.getValue());
                } catch (final RepositoryException e) {
                    throw propagate(e);
                } catch (final Exception e) {
                    throw propagate(e);
                }
            }
        };

    /**
     * Check if a JCR property is a multivalued property or not
     */
    public static Predicate<Property> isMultipleValuedProperty =
        new Predicate<Property>() {

            @Override
            public boolean apply(final Property p) {
                checkNotNull(p, "null is neither multiple nor not multiple!");
                try {
                    return p.isMultiple();
                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }
        };

    /**
     * Check if a JCR property is a binary jcr:data property
     */
    public static Predicate<Property> isBinaryContentProperty =
        new Predicate<Property>() {

            @Override
            public boolean apply(final Property p) {
                checkNotNull(p, "null is neither binary nor not binary!");
                try {
                    return p.getType() == BINARY && p.getName().equals(JCR_DATA);
                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }
        };

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
                    throw propagate(e);
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
                return isReferenceProperty.apply(p) || isBinaryContentProperty.apply(p)
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
                } catch (RepositoryException e) {
                    throw propagate(e);
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
                final NodeType primaryNodeType = n.getPrimaryNodeType();
                return primaryNodeType != null
                        && primaryNodeType.isNodeType("mode:system");
            } catch (final RepositoryException e) {
                throw propagate(e);
            }
        }
    };

    /**
     * ISODateTimeFormat is thread-safe and immutable, and the formatters it
     * returns are as well.
     */
    private static final DateTimeFormatter FMT = ISODateTimeFormat.dateTime();

    /**
     * Get the JCR Node Type manager
     *
     * @param node
     * @return the JCR Node Type Manager
     * @throws RepositoryException
     */
    public static NodeTypeManager getNodeTypeManager(final Node node) throws RepositoryException {
        return node.getSession().getWorkspace().getNodeTypeManager();
    }

    /**
     * Get the property definition information (containing type and multi-value
     * information)
     *
     * @param node the node to use for inferring the property definition
     * @param propertyName the property name to retrieve a definition for
     * @return a JCR PropertyDefinition, if available, or null
     * @throws javax.jcr.RepositoryException
     */
    public static PropertyDefinition getDefinitionForPropertyName(final Node node,
        final String propertyName) throws RepositoryException {

        final PropertyDefinition[] propertyDefinitions =
            node.getPrimaryNodeType().getPropertyDefinitions();
        LOGGER.debug("Looking for property name: {}", propertyName);
        for (final PropertyDefinition p : propertyDefinitions) {
            LOGGER.debug("Checking property: {}", p.getName());
            if (p.getName().equals(propertyName)) {
                return p;
            }
        }

        for (final NodeType nodeType : node.getMixinNodeTypes()) {
            for (final PropertyDefinition p : nodeType.getPropertyDefinitions()) {
                if (p.getName().equals(propertyName)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Convenience method for transforming arrays into {@link Collection}s
     * through a mapping {@link Function}.
     *
     * @param input A {@literal Collection<F> }.
     * @param f A {@literal Function<F,T> }.
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
     * Get the JCR VersionHistory for a node at a given JCR path
     * 
     * @param session
     * @param path
     * @return the version history
     * @throws RepositoryException
     */
    public static VersionHistory getVersionHistory(final Session session,
            final String path) throws RepositoryException {
        return session.getWorkspace().getVersionManager().getVersionHistory(
                path);
    }

    /**
     * Check if the property contains the given string value
     * 
     * @param p
     * @param value
     * @return true if the property contains the given string value
     */
    public static boolean propertyContains(final Property p, final String value) throws RepositoryException {

        if (p == null) {
            return false;
        }

        if (p.isMultiple()) {
            return contains(transform(forArray(p.getValues()), value2string), value);
        }
        return value.equals(p.getString());

    }
    /**
     * Check if there is a node of given type
     * @param subjectNode
     * @param mixinName
     */
    public static boolean nodeHasType(final Node subjectNode, final String mixinName) throws RepositoryException {
        if (subjectNode == null) {
            return false;
        }
        return getNodeTypeManager(subjectNode).hasNodeType(mixinName);
    }
}
