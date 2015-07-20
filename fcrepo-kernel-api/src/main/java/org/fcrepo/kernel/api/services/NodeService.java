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
package org.fcrepo.kernel.api.services;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Session;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;

/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface NodeService extends Service<FedoraResource> {
    /**
     * Copy an existing object from the source path to the destination path
     * @param session the session
     * @param source the source
     * @param destination the destination
     */
    void copyObject(Session session, String source, String destination);

    /**
     * Move an existing object from the source path to the destination path
     * @param session the session
     * @param source the source
     * @param destination the destination
     */
    void moveObject(Session session, String source, String destination);

    /**
     * @param session the session
     * @return RDFStream of node types
     */
    RdfStream getNodeTypes(final Session session);

    /**
     * @param session the session
     * @param cndStream the cnd stream
     * @throws IOException if IO exception occurred
     */
    void registerNodeTypes(final Session session, final InputStream cndStream) throws IOException;
}
