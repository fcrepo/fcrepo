/**
 *
 */

package org.fcrepo.kernel.identifiers;

/**
 * Translates internal {@link String} identifiers to internal {@link String}
 * identifiers.
 *
 * @author ajs6f
 * @date Apr 1, 2014
 */
public abstract class InternalIdentifierTranslator extends IdentifierTranslator<String> {

    private static final InternalIdentifierTranslator identity = new InternalIdentifierTranslator() {};

    /*
     * (non-Javadoc)
     * @see com.google.common.base.Converter#doForward(java.lang.Object)
     */
    @Override
    protected String doForward(final String a) {
        return a;
    }

    /*
     * (non-Javadoc)
     * @see com.google.common.base.Converter#doBackward(java.lang.Object)
     */
    @Override
    protected String doBackward(final String b) {
        return b;
    }

    public static InternalIdentifierTranslator identityTranslation() {
        return identity;
    }

}
