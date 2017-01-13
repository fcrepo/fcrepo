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

package org.fcrepo.http.commons.exceptionhandlers;

import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.exception.ConstraintViolationException;

/**
 * Abstract class for constraint violation subclasses
 *
 * @author whikloj
 * @since 2015-06-24
 * @param <T> Throwable subclass of ConstraintViolationException
 */
public abstract class ConstraintExceptionMapper<T extends ConstraintViolationException>
        extends FedoraExceptionMapper<T> {

    @Context
    private UriInfo uriInfo;

    /**
     * Where the RDF exception files sit.
     */
    private static final String CONSTRAINT_DIR = "/static/constraints/";

    /**
     * Creates a constrainedBy link header with the appropriate RDF URL for the exception.
     *
     * @param e ConstraintViolationException Exception which implements the buildContraintUri method.
     * @param uriInfo UriInfo UriInfo from the ExceptionMapper.
     * @return Link A http://www.w3.org/ns/ldp#constrainedBy link header
     */
    public static Link buildConstraintLink(final ConstraintViolationException e, final UriInfo uriInfo) {
        final String constraintURI = uriInfo == null ? "" : String.format("%s://%s%s%s.rdf",
                uriInfo.getBaseUri().getScheme(), uriInfo.getBaseUri().getAuthority(),
                CONSTRAINT_DIR, e.getClass().toString().substring(e.getClass().toString().lastIndexOf('.') + 1));
        return Link.fromUri(constraintURI).rel(CONSTRAINED_BY.getURI()).build();
    }

    @Override
    protected ResponseBuilder links(final ResponseBuilder builder, final T e) {
        return builder.links(buildConstraintLink(e, uriInfo));
    }

}
