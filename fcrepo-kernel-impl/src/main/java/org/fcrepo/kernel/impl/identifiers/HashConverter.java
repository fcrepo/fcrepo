package org.fcrepo.kernel.impl.identifiers;

import org.fcrepo.kernel.identifiers.InternalIdentifierConverter;

/**
 * @author cabeer
 * @since 10/9/14
 */
public class HashConverter extends InternalIdentifierConverter {

    @Override
    protected String doForward(final String externalId) {
        return externalId.replace("#", "/#");
    }

    @Override
    protected String doBackward(final String internalId) {
        return internalId.replace("/#", "#");

    }
}
