/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services.functions;

import java.util.function.Supplier;

/**
 * A {@link java.util.function.Supplier} interface that guarantees the uniqueness of its provided values.
 *
 * @author acoburn
 */
public interface UniqueValueSupplier extends Supplier<String> {

}
