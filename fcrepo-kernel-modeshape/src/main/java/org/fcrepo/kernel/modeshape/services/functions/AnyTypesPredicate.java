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

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.fcrepo.kernel.modeshape.utils.UncheckedPredicate.uncheck;

import java.util.Collection;
import javax.jcr.Node;

import org.fcrepo.kernel.modeshape.utils.UncheckedPredicate;


/**
 * Predicate to match nodes with any of the given mixin types
 * @author armintor@gmail.com
 * @author ajs6f
 *
 */
public class AnyTypesPredicate implements UncheckedPredicate<Node> {
    private final Collection<String> nodeTypes;

    /**
     * True if any of the types specified match.
     * @param types the types
     */
    public AnyTypesPredicate(final String...types) {
        nodeTypes = asList(types);
    }

    @Override
    public boolean testThrows(final Node input) {
        requireNonNull(input, "null node has no types!");
        return nodeTypes.stream().anyMatch(uncheck(input::isNodeType));
    }
}
