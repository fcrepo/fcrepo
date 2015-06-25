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
package org.fcrepo.kernel.impl;

import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isContainer;

import javax.jcr.Node;

import org.fcrepo.kernel.models.Container;

/**
 * An abstraction that represents a Fedora Object backed by
 * a JCR node.
 *
 * @author ajs6f
 * @since Feb 21, 2013
 */
public class ContainerImpl extends FedoraResourceImpl implements Container {


    /**
     * Construct a {@link org.fcrepo.kernel.models.Container} from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public ContainerImpl(final Node node) {
        super(node);
    }

    /**
     * Check if the node has a fedora:object mixin
     * @param node the given node
     * @return true if the node has the fedora object mixin
     */
    public static boolean hasMixin(final Node node) {
        return isContainer.test(node);
    }

}
