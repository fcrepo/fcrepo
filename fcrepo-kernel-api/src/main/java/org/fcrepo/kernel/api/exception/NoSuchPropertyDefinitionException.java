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
package org.fcrepo.kernel.api.exception;

import javax.jcr.RepositoryException;

/**
 * Represents the case where a property definition has been requested but does
 * not exist. Typically, this happens when a new property is added to a node
 * that does not restrict its property types.
 *
 * @author ajs6f
 * @since Oct 25, 2013
 */
public class NoSuchPropertyDefinitionException extends RepositoryException {

    private static final long serialVersionUID = 1L;

}
