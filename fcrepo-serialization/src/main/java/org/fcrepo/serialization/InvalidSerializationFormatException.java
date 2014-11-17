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
package org.fcrepo.serialization;

/**
 * Exception thrown when during deserialization it becomes obvious that the
 * InputStream in not in the expected format.
 * @author md5wz
 * @since November 2014
 */
public class InvalidSerializationFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Exception with message
     * @param message
     */
    public InvalidSerializationFormatException(final String message) {
        super(message);
    }

}
