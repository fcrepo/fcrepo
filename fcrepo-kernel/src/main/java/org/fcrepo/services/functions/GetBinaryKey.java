/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services.functions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

import com.google.common.base.Function;

/**
 * @todo Add Documentation.
 * @author barmintor
 * @date Apr 2, 2013
 */
public class GetBinaryKey implements Function<Property, BinaryKey> {

    /**
     * @todo Add Documentation.
     */
    @Override
    public BinaryKey apply(final Property input) {
        checkArgument(input != null, "null cannot have a Binarykey!");
        try {
            return ((BinaryValue) input.getBinary()).getKey();
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

}
