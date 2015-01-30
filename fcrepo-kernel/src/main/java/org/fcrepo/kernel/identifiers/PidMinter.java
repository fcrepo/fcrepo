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
package org.fcrepo.kernel.identifiers;

/**
 * Defines the behavior of a component that can accept responsibility
 * for the creation of Fedora PIDs.
 *
 * @author eddies
 * @author ajs6f
 * @since Feb 7, 2013
 */
public interface PidMinter {

    /**
     * Mint a new PID
     * @return a new identifier
     */
    String mintPid();

}
