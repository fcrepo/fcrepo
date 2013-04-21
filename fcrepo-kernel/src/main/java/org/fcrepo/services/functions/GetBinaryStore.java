
package org.fcrepo.services.functions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;

import javax.jcr.Repository;

import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.value.binary.BinaryStore;

import com.google.common.base.Function;

public class GetBinaryStore implements Function<Repository, BinaryStore> {

    /**
     * Extract the BinaryStore out of Modeshape (infinspan, jdbc, file, transient, etc)
     * @return
     */
    @Override
    public BinaryStore apply(final Repository input) {
        checkArgument(input != null, "null cannot have a BinaryStore!");
        try {
            return ((JcrRepository) input).getConfiguration()
                    .getBinaryStorage().getBinaryStore();
        } catch (final Exception e) {
            throw propagate(e);
        }
    }

}
