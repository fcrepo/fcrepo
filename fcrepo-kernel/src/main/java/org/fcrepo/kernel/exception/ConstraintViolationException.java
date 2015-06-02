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
package org.fcrepo.kernel.exception;

import static org.fcrepo.kernel.RdfLexicon.CONSTRAINED_BY;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriInfo;

/**
 * A constraint has been violated.
 *
 * @author whikloj
 * @since 2015-05-29
 */
public class ConstraintViolationException extends MalformedRdfException {

    private static final long serialVersionUID = 1L;

    private static final String CONSTRAINT_DIR = "/static/constraints/";

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public ConstraintViolationException(final String msg) {
        super(msg);
    }

    /**
     * Ordinary constructor.
     *
     * @param rootCause the root cause
     */
    public ConstraintViolationException(final Throwable rootCause) {
        super(rootCause);
    }

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public ConstraintViolationException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }

    /**
     * Generates a link header for a constraint based on UriInfo and the name of the Exception class.
     * @return Link
     */
    public Link buildConstraintLink(final UriInfo uriInfo) {
        final String constraintURI =
            uriInfo == null ? "" : String.format("%s://%s%s%s.rdf", uriInfo.getBaseUri().getScheme(), uriInfo
                .getBaseUri().getAuthority(),
                CONSTRAINT_DIR, this.getClass().toString().substring(this.getClass().toString().lastIndexOf('.') + 1));
        return Link.fromUri(constraintURI).rel(CONSTRAINED_BY.getURI()).build();

    }
}
