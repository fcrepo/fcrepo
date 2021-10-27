/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.common.retry;

/**
 * Tests if a method should be retried based on the exception thrown
 *
 * @author pwinckles
 */
public interface ExceptionChecker {

    /**
     * Return true if the method that produced the exception should be retried
     *
     * @param e the exception
     * @return true if the method should be retried
     */
    boolean shouldRetry(Exception e);

}
