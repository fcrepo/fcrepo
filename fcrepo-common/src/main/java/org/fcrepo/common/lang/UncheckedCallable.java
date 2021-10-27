/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.common.lang;

/**
 * Same as Callable but it does not have Exception in its signature
 *
 * @author pwinckles
 */
public interface UncheckedCallable<T> {
    T call();
}
