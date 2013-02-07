package org.fcrepo.modeshape.utils;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.core.Response;

/**
 * Convenience class for constructs useful to many JAX-RS methods.
 * 
 * @author ajs6f
 * 
 */
public abstract class Constants {

	protected static final Response four03 = status(FORBIDDEN).build();
	protected static final Response four04 = status(NOT_FOUND).build();
}
