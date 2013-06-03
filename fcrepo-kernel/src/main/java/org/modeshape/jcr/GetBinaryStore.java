/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.modeshape.jcr;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;

import javax.jcr.Repository;

import org.modeshape.jcr.value.binary.BinaryStore;

import com.google.common.base.Function;

/**
 * @todo Add Documentation.
 * @author cbeer
 * @date Apr 30, 2013
 */
public class GetBinaryStore implements Function<Repository, BinaryStore> {

    /**
     * Extract the BinaryStore out of Modeshape
     * (infinspan, jdbc, file, transient, etc)
     * @return
     */
    @Override
    public BinaryStore apply(final Repository input) {
        checkArgument(input != null, "null cannot have a BinaryStore!");
        try {
            assert(input != null);
            JcrRepository.RunningState runningState = ((JcrRepository)input).
                runningState();

            return runningState.binaryStore();
        } catch (final Exception e) {
            throw propagate(e);
        }
    }

}
