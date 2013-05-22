
package org.fcrepo.services.functions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

import com.google.common.base.Function;

public class GetBinaryKey implements Function<Property, BinaryKey> {

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
