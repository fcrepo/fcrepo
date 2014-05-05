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

/**
 * @author Mike Durbin
 */
public interface Lock {

    /**
     * Gets the lock token that serves to authorize access to
     * resources locked by this lock.  (May not be available
     * in all cases).
     */
    public String getLockToken();

    /**
     * Gets whether this lock is deep, in that it affects the
     * entire subgraph under the locked node.
     */
    public boolean isDeep();
}
