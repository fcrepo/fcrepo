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
package org.fcrepo.kernel.api.exception;

/**
 * Exception to signal a received string is not a valid media type.
 *
 * @author claussni
 */
public class InvalidMediaTypeException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified media type string.
     * The exception message is going to be "Invalid media type ..."
     *
     * @param mediaTypeString the supposedly invalid media type string
     */
    public InvalidMediaTypeException(final String mediaTypeString) {
        super(String.format("Invalid media type `%s`", mediaTypeString));
    }

}
