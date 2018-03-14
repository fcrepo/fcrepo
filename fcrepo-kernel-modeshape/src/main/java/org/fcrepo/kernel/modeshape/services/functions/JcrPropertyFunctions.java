/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.services.functions;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.fcrepo.kernel.modeshape.utils.UncheckedFunction;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;
import static javax.jcr.PropertyType.BINARY;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FROZEN_NODE;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.fcrepo.kernel.modeshape.utils.UncheckedPredicate.uncheck;

/**
 * @author cabeer
 * @since 9/25/14
 */
public final class JcrPropertyFunctions {

    private JcrPropertyFunctions() {
    }

    /**
     * Constructs an {@link java.util.stream.Stream} of {@link javax.jcr.Value}s from any {@link javax.jcr.Property},
     * multi- or single-valued.
     */
    public static Function<Property, Stream<Value>> property2values = (Function<Property, Stream<Value>>)
            UncheckedFunction.uncheck((final Property p) -> p.isMultiple() ? of(p.getValues()) : of(p.getValue()));

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
