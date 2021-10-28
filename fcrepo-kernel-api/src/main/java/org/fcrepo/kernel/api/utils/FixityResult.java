/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.utils;

import java.net.URI;
import java.util.Set;

/**
 * @author bbpennel
 * @since Feb 18, 2014
 */
public interface FixityResult {

    /**
     * The possible fixity states (which may be ORed together later)
     */
    enum FixityState {
        SUCCESS, BAD_CHECKSUM, BAD_SIZE
    }

    /**
     * Check if the fixity result matches the given checksum URI
     *
     * @param checksum the given checksum uri
     * @return fixity result matches the given checksum URI
     */
    boolean matches(URI checksum);

    /**
     * Check if the fixity result matches the given size
     *
     * @param size the given size
     * @return fixity result matches the given size
     */
    boolean matches(long size);

    /**
     * Does the fixity entry match the given size and checksum?
     *
     * @param size bitstream size in bytes
     * @param checksum checksum URI
     * @return true if both conditions matched
     */
    boolean matches(long size, URI checksum);

    /**
     * @param size the size
     * @param checksum the checksum uri
     * @return the status
     */
    Set<FixityState> getStatus(long size, URI checksum);

    /**
     * @return the computed size
     */
    long getComputedSize();

    /**
     * @return the computed checksum
     */
    URI getComputedChecksum();

    /**
     * @return the algorithm uses to compute the checksum
     */
    String getUsedAlgorithm();

}
