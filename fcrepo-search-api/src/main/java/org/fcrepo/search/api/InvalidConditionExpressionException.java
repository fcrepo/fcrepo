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
package org.fcrepo.search.api;

/**
 * An exception that indicates that the syntax of the condition expression
 * is invalid and therefore cannot be parsed.
 *
 * @author dbernstein
 */
public class InvalidConditionExpressionException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Basic constructor
     *
     * @param msg The text of the exception.
     */
    public InvalidConditionExpressionException(final String msg) {
        super(msg);
    }

}
