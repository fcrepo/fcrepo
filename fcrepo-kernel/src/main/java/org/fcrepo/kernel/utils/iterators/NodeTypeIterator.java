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
package org.fcrepo.kernel.utils.iterators;

import com.google.common.collect.ForwardingIterator;

import javax.jcr.nodetype.NodeType;
import java.util.Iterator;


/**
 *
 * A type-aware iterator that wraps the generic JCR {@link javax.jcr.nodetype.NodeTypeIterator}
 *
 * @author cbeer
 */
public class NodeTypeIterator extends ForwardingIterator<NodeType> implements
    Iterable<NodeType> {

    private javax.jcr.nodetype.NodeTypeIterator i;

    /**
     * Wrap the NodeIterator with our generic version
     *
     * @param i
     */
    public NodeTypeIterator(final javax.jcr.nodetype.NodeTypeIterator i) {
        this.i = i;
    }

    @Override
    public Iterator<NodeType> iterator() {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Iterator<NodeType> delegate() {
        return i;
    }

}
