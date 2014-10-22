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
package org.fcrepo.kernel.services;

import org.fcrepo.kernel.FedoraBinary;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * @author cabeer
 * @since 10/10/14
 */
public interface BinaryService {

    /**
     * Retrieve a Binary instance by session and path
     *
     * @param path jcr path to the datastream
     * @return retrieved Datastream
     */
    FedoraBinary findOrCreateBinary(final Session session, final String path);

    /**
     * Retrieve a Binary instance from a node
     *
     * @param node datastream node
     * @return node as a Datastream
     */
    FedoraBinary asBinary(Node node);
}
