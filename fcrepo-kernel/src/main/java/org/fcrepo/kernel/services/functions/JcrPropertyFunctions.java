/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.services.functions;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import org.fcrepo.kernel.FedoraJcrTypes;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static javax.jcr.PropertyType.BINARY;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/25/14
 */
public abstract class JcrPropertyFunctions {

    private static final Logger LOGGER = getLogger(JcrPropertyFunctions.class);
    /**
     * Translates a {@link javax.jcr.nodetype.NodeType} to its {@link String} name.
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
     * Translates a JCR {@link javax.jcr.Value} to its {@link String} expression.
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
     * Constructs an {@link java.util.Iterator} of {@link javax.jcr.Value}s from any
     * {@link javax.jcr.Property}, multi-valued or not.
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
     * Predicate for determining whether this {@link javax.jcr.Node} is a frozen node
     * (a part of the system version history).
     */
    public static Predicate<Node> isFrozen = new Predicate<Node>() {

        @Override
        public boolean apply(final Node node) {
            checkNotNull(node, "null cannot be a Frozen node!");
            try {
                return node.isNodeType(FedoraJcrTypes.FROZEN_NODE);
            } catch (final RepositoryException e) {
                throw propagate(e);
            }
        }
    };
}
