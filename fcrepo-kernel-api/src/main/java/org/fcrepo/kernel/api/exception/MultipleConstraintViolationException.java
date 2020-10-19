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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper to hold multiple constraint violation exceptions for later reporting.
 * @author whikloj
 */
public class MultipleConstraintViolationException extends ConstraintViolationException {

    private Set<ConstraintViolationException> exceptionTypes = new HashSet<>();

    private String fullMessage = "";

    public Set<ConstraintViolationException> getExceptionTypes() {
        return exceptionTypes;
    }

    public MultipleConstraintViolationException(final List<ConstraintViolationException> exceptions) {
        super("There are multiple exceptions");
        for (final ConstraintViolationException exception : exceptions) {
            exceptionTypes.add(exception);
            fullMessage += exception.getMessage() + "\n";
        }
    }

    @Override
    public String getMessage() {
        return fullMessage;
    }

}
