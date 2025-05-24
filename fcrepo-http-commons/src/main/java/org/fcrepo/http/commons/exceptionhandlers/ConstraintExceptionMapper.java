/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.fcrepo.kernel.api.exception.ConstraintViolationException;

/**
 * Abstract class for constraint violation subclasses
 *
 * @author whikloj
 * @since 2015-06-24
 * @param <T> Throwable subclass of ConstraintViolationException
 */
public abstract class ConstraintExceptionMapper<T extends ConstraintViolationException> implements ExceptionMapper<T> {

    /**
     * Where the RDF exception files sit.
     */
    private static final String CONSTRAINT_DIR = "/static/constraints/";

    /**
     * Creates a constrainedBy link header with the appropriate RDF URL for the exception.
     *
     * @param e ConstraintViolationException Exception which implements the buildContraintUri method.
     * @param context ServletContext ServletContext that we're running in.
     * @param uriInfo UriInfo UriInfo from the ExceptionMapper.
     * @return Link A http://www.w3.org/ns/ldp#constrainedBy link header
     */
    public static Link buildConstraintLink(final ConstraintViolationException e,
                                           final ServletContext context,
                                           final UriInfo uriInfo) {
        return buildConstraintLink(e.getClass(), context, uriInfo);
    }

    /**
     * Creates a constrainedBy link header with the appropriate RDF URL for the exception.
     *
     * @param clazz the class of the exception to build the link for.
     * @param context ServletContext ServletContext that we're running in.
     * @param uriInfo UriInfo UriInfo from the ExceptionMapper.
     * @return Link A http://www.w3.org/ns/ldp#constrainedBy link header
     */
    public static Link buildConstraintLink(final Class<? extends ConstraintViolationException> clazz,
                                           final ServletContext context,
                                           final UriInfo uriInfo) {
        String path = context.getContextPath();
        if (path.equals("/")) {
            path = "";
        }

        final String constraintURI = uriInfo == null ? "" : String.format("%s://%s%s%s%s.rdf",
                uriInfo.getBaseUri().getScheme(), uriInfo.getBaseUri().getAuthority(), path,
                CONSTRAINT_DIR, clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1));
        return Link.fromUri(constraintURI).rel(CONSTRAINED_BY.getURI()).build();
    }

}
