/**
 *
 */

package org.fcrepo.kernel.identifiers;

import com.google.common.base.Converter;

/**
 * An {@link IdentiferTranslator} accepts and returns identifiers, translating
 * them in some type-specific manner. The typical use of this
 * contract is for translating between internal and external identifiers.
 *
 * @author ajs6f
 * @date Mar 26, 2014
 * @param <T> the type to and from which we are translating
 */
public abstract class IdentifierTranslator<T> extends Converter<String, T> {

}
