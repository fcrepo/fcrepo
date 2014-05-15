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
package org.fcrepo.kernel;

import javax.jcr.RepositoryException;

/**
 * @author bbpennel
 * @since Feb 18, 2014
 */
public interface FedoraObject extends FedoraResource {

    /**
     * @return The JCR name of the node that backs this object.
     * @throws RepositoryException
     */
    String getName() throws RepositoryException;

}