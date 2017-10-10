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
package org.fcrepo.kernel.modeshape;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraMemento;

/**
 * @author mohideen
 * @since Oct. 05, 2017
 */
public class FedoraMementoImpl extends FedoraResourceImpl implements FedoraMemento {


    /**
     * Construct a {@link org.fcrepo.kernel.api.models.FedoraResource} from an existing JCR Node
     * @param node an existing JCR node to be treated as a fcrepo object
     */
    public FedoraMementoImpl(final Node node) {
        super(node);
    }

    /**
     * Check if the JCR node has a fedora:Memento mixin
     * @param node the JCR node
     * @return true if the JCR node has the fedora:Memento mixin
     */
    public static boolean hasMixin(final Node node) {
        try {
            return node.isNodeType(FEDORA_MEMENTO);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
