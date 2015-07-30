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
package org.fcrepo.kernel.api.services.functions;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.fcrepo.kernel.api.utils.UncheckedFunction;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.collect.Iterators.forArray;
import static com.google.common.collect.Iterators.singletonIterator;
import static javax.jcr.PropertyType.BINARY;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FROZEN_NODE;
import static org.fcrepo.kernel.api.utils.UncheckedPredicate.uncheck;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

/**
 * @author cabeer
 * @since 9/25/14
 */
public final class JcrPropertyFunctions {

    private JcrPropertyFunctions() {
    }

    /**
     * Constructs an {@link java.util.Iterator} of {@link javax.jcr.Value}s from any {@link javax.jcr.Property},
     * multi- or single-valued.
     */
    public static Function<Property, Iterator<Value>> property2values = UncheckedFunction.uncheck(
            (final Property p) -> p.isMultiple() ? forArray(p.getValues()) : singletonIterator(p.getValue()));

    /**
     * Check if a JCR property is a binary jcr:data property
     */
    public static Predicate<Property> isBinaryContentProperty = uncheck(p -> p.getType() == BINARY &&
            p.getName().equals(JCR_DATA));

    /**
     * Predicate for determining whether this {@link javax.jcr.Node} is a frozen node
     * (a part of the system version history).
     */
    public static Predicate<Node> isFrozen = uncheck(n -> n.isNodeType(FROZEN_NODE));

}
