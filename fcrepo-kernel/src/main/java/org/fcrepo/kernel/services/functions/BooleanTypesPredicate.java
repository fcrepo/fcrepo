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

import static com.google.common.base.Throwables.propagate;
import static org.fcrepo.kernel.FedoraJcrTypes.FROZEN_MIXIN_TYPES;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.isFrozen;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.property2values;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.value2string;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

/**
 * Base class for matching sets of node types
 * @author armintor@gmail.com
 *
 */
public abstract class BooleanTypesPredicate implements Predicate<Node> {

    protected final Collection<String> nodeTypes;

    /**
     * Base constructor for function peforming boolean ops on matched node types.
     * @param types
     */
    public BooleanTypesPredicate(final String... types) {
        nodeTypes = Arrays.asList(types);
    }

    @Override
    public boolean apply(final Node input) {
        if (input == null) {
            throw new IllegalArgumentException(
                    "null node passed to" + getClass().getName()
            );
        }
        int matched = 0;
        try {

            if (isFrozen.apply(input) && input.hasProperty(FROZEN_MIXIN_TYPES)) {
                final Iterator<String> transform = Iterators.transform(
                        property2values.apply(input.getProperty(FROZEN_MIXIN_TYPES)),
                        value2string
                );

                while (transform.hasNext()) {
                    if (nodeTypes.contains(transform.next())) {
                        matched++;
                    }
                }
            } else {
                for (final String nodeType : nodeTypes) {
                    if (input.isNodeType(nodeType)) {
                        matched++;
                    }
                }
            }
        } catch (RepositoryException e) {
            throw propagate(e);
        }
        return test(matched);
    }

    protected abstract boolean test(final int matched);

}
